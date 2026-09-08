package com.jinlee.devtroublemate.ai.service;

import com.jinlee.devtroublemate.ai.domain.AIProcessingStatus;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.repository.AIAnalysisRepository;
import com.jinlee.devtroublemate.embedding.service.EmbeddingService;
import com.jinlee.devtroublemate.tag.repository.TroubleTagRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAIProviderStubIntegrationTest {

    private final TroubleRepository troubleRepository = mock(TroubleRepository.class);
    private final AIAnalysisRepository aiAnalysisRepository = mock(AIAnalysisRepository.class);
    private final EmbeddingService embeddingService = mock(EmbeddingService.class);
    private final TroubleTagRepository troubleTagRepository = mock(TroubleTagRepository.class);

    private MockWebServer openAIStub;
    private OpenAIService openAIService;
    private AIAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() throws IOException {
        openAIStub = new MockWebServer();
        openAIStub.start();
        openAIService = createOpenAIService();
        aiAnalysisService = new AIAnalysisService(
                openAIService,
                aiAnalysisRepository,
                troubleRepository,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                embeddingService,
                troubleTagRepository
        );
    }

    @AfterEach
    void stopOpenAIStub() throws IOException {
        openAIStub.shutdown();
    }

    @Test
    void callProviderAndParseValidResponseWithoutRealOpenAIRequest() throws InterruptedException {
        enqueueChatCompletion(validAnalysisJson());

        AIAnalysisResponse response = analyze();

        assertThat(response.category()).isEqualTo("인증");
        assertThat(response.confidence()).isEqualTo(92);
        RecordedRequest request = openAIStub.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/v1/chat/completions");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer stub-key");
        assertThat(request.getBody().readUtf8()).contains("JWT 인증 실패");
    }

    @Test
    void rejectMalformedAnalysisJsonReturnedByProvider() {
        enqueueChatCompletion("not-json");

        assertThatThrownBy(this::analyze)
                .isInstanceOfSatisfying(AIServiceException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AI_RESPONSE_INVALID"));
        assertThat(openAIStub.getRequestCount()).isEqualTo(1);
    }

    @Test
    void retryRateLimitAndSucceedOnSecondAttempt() {
        openAIStub.enqueue(errorResponse(429, "rate_limit_exceeded"));
        enqueueChatCompletion(validAnalysisJson());

        AIAnalysisResponse response = analyze();

        assertThat(response.summary()).isEqualTo("JWT 토큰 검증 실패");
        assertThat(openAIStub.getRequestCount()).isEqualTo(2);
    }

    @Test
    void translateRateLimitAfterRetryIsExhaustedAndRecordFailedStatus() {
        openAIStub.enqueue(errorResponse(429, "rate_limit_exceeded"));
        openAIStub.enqueue(errorResponse(429, "rate_limit_exceeded"));
        Trouble trouble = trouble();

        assertThatThrownBy(() -> aiAnalysisService.analyzeAndSave(
                trouble, trouble.getTitle(), trouble.getDescription(), trouble.getRawLog(), List.of("JWT")))
                .isInstanceOfSatisfying(AIServiceException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("OPENAI_RATE_LIMITED"));

        assertThat(openAIStub.getRequestCount()).isEqualTo(2);
        assertThat(trouble.getAiProcessingStatus()).isEqualTo(AIProcessingStatus.FAILED);
        assertThat(trouble.getAiLastErrorCode()).isEqualTo("OPENAI_RATE_LIMITED");
        verify(troubleRepository, times(2)).save(trouble);
    }

    @Test
    void doNotRetryAuthenticationFailureAndRecordFailedStatus() {
        openAIStub.enqueue(errorResponse(401, "invalid_api_key"));
        Trouble trouble = trouble();

        assertThatThrownBy(() -> aiAnalysisService.analyzeAndSave(
                trouble, trouble.getTitle(), trouble.getDescription(), trouble.getRawLog(), List.of("JWT")))
                .isInstanceOfSatisfying(AIServiceException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("OPENAI_AUTHENTICATION_FAILED"));

        assertThat(openAIStub.getRequestCount()).isEqualTo(1);
        assertThat(trouble.getAiProcessingStatus()).isEqualTo(AIProcessingStatus.FAILED);
        assertThat(trouble.getAiLastErrorCode()).isEqualTo("OPENAI_AUTHENTICATION_FAILED");
    }

    @Test
    void translateProviderTimeoutAndRecordFailedStatus() {
        openAIStub.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeadersDelay(500, TimeUnit.MILLISECONDS)
                .setBody(chatCompletion(validAnalysisJson())));
        Trouble trouble = trouble();

        assertThatThrownBy(() -> aiAnalysisService.analyzeAndSave(
                trouble, trouble.getTitle(), trouble.getDescription(), trouble.getRawLog(), List.of("JWT")))
                .isInstanceOfSatisfying(AIServiceException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("OPENAI_TIMEOUT"));

        assertThat(openAIStub.getRequestCount()).isEqualTo(1);
        assertThat(trouble.getAiProcessingStatus()).isEqualTo(AIProcessingStatus.FAILED);
        assertThat(trouble.getAiLastErrorCode()).isEqualTo("OPENAI_TIMEOUT");
    }

    @Test
    void completeStateAndCreateEmbeddingAfterSuccessfulProviderResponse() {
        enqueueChatCompletion(validAnalysisJson());
        Trouble trouble = trouble();
        when(aiAnalysisRepository.findTopByTroubleOrderByCreatedAtDesc(trouble))
                .thenReturn(java.util.Optional.empty());

        AIAnalysisResponse response = aiAnalysisService.analyzeAndSave(
                trouble, trouble.getTitle(), trouble.getDescription(), trouble.getRawLog(), List.of("JWT"));

        assertThat(trouble.getAiProcessingStatus()).isEqualTo(AIProcessingStatus.COMPLETED);
        assertThat(trouble.getAiLastErrorCode()).isNull();
        verify(embeddingService).createOrUpdate(trouble, response);
    }

    private OpenAIService createOpenAIService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(200));
        requestFactory.setReadTimeout(Duration.ofMillis(200));
        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);
        SpringAiRetryProperties retryProperties = new SpringAiRetryProperties();
        retryProperties.setMaxAttempts(2);
        retryProperties.setOnHttpCodes(List.of(429));
        retryProperties.getBackoff().setInitialInterval(Duration.ofMillis(1));
        retryProperties.getBackoff().setMaxInterval(Duration.ofMillis(2));
        SpringAiRetryAutoConfiguration retryConfiguration = new SpringAiRetryAutoConfiguration();

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(openAIStub.url("/").toString())
                .apiKey(new SimpleApiKey("stub-key"))
                .restClientBuilder(restClientBuilder)
                .responseErrorHandler(retryConfiguration.responseErrorHandler(retryProperties))
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").build())
                .retryTemplate(retryConfiguration.retryTemplate(retryProperties))
                .build();

        return new OpenAIService(ChatClient.builder(chatModel), new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private AIAnalysisResponse analyze() {
        return openAIService.analyzeTroubleLog(
                "JWT 인증 실패", "로그인 요청이 거부됨", "token expired", List.of("JWT"));
    }

    private Trouble trouble() {
        return Trouble.builder()
                .title("JWT 인증 실패")
                .description("로그인 요청이 거부됨")
                .rawLog("token expired")
                .build();
    }

    private void enqueueChatCompletion(String content) {
        openAIStub.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(chatCompletion(content)));
    }

    private MockResponse errorResponse(int status, String code) {
        return new MockResponse()
                .setResponseCode(status)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"error":{"message":"stub error","type":"%s","code":"%s"}}
                        """.formatted(code, code));
    }

    private String chatCompletion(String content) {
        String escapedContent = new com.fasterxml.jackson.databind.ObjectMapper()
                .valueToTree(content)
                .toString();
        return """
                {
                  "id":"chatcmpl-stub",
                  "object":"chat.completion",
                  "created":1,
                  "model":"gpt-4o-mini",
                  "choices":[{"index":0,"message":{"role":"assistant","content":%s},"finish_reason":"stop"}],
                  "usage":{"prompt_tokens":10,"completion_tokens":20,"total_tokens":30}
                }
                """.formatted(escapedContent);
    }

    private String validAnalysisJson() {
        return """
                {"category":"인증","summary":"JWT 토큰 검증 실패","possibleCauses":["토큰 만료"],"runbook":["토큰 만료 시간 확인"],"confidence":92}
                """;
    }

}

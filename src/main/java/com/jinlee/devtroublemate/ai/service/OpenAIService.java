package com.jinlee.devtroublemate.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
import com.jinlee.devtroublemate.ai.exception.AIExceptionTranslator;
import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.prompt.TroubleAnalysisPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenAIService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public OpenAIService(
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public AIAnalysisResponse analyzeTroubleLog(
            String title,
            String description,
            String rawLog,
            List<String> tags
    ) {
        String prompt = TroubleAnalysisPrompt.render(title, description, rawLog, tags);

        String result;
        try {
            result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (RuntimeException exception) {
            throw AIExceptionTranslator.forAnalysis(exception);
        }

        try {
            AIAnalysisResponse response = objectMapper.readValue(result, AIAnalysisResponse.class);
            validate(response);
            return response;
        } catch (JsonProcessingException e) {
            throw AIServiceException.invalidResponse(e);
        }
    }

    private void validate(AIAnalysisResponse response) {
        if (response == null
                || isBlank(response.category())
                || isBlank(response.summary())
                || response.possibleCauses() == null
                || response.possibleCauses().isEmpty()
                || response.possibleCauses().size() > 3
                || response.possibleCauses().stream().anyMatch(this::isBlank)
                || response.runbook() == null
                || response.runbook().isEmpty()
                || response.runbook().size() > 3
                || response.runbook().stream().anyMatch(this::isBlank)
                || response.confidence() == null
                || response.confidence() < 0
                || response.confidence() > 100) {
            throw AIServiceException.invalidResponse(null);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}

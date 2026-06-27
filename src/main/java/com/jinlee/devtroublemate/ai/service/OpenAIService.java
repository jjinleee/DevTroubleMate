package com.jinlee.devtroublemate.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
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
        String prompt = createTroubleAnalysisPrompt(title, description, rawLog, tags);

        String result = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        try {
            return objectMapper.readValue(result, AIAnalysisResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 응답 파싱 실패", e);
        }
    }

    private String createTroubleAnalysisPrompt(
            String title,
            String description,
            String rawLog,
            List<String> tags
    ) {
        return """
                너는 백엔드 장애 로그를 분석하는 AI야.
                아래 장애 정보를 분석해서 JSON 형식으로만 응답해.

                조건:
                - 모르는 내용은 단정하지 말 것
                - 로그에서 확인 가능한 근거를 우선할 것
                - 원인 후보는 최대 3개만 작성할 것
                - 점검 순서는 실제 개발자가 확인할 수 있는 행동 중심으로 작성할 것
                - 마크다운 코드블록을 사용하지 말 것
                - 설명 문장을 추가하지 말 것
                - 반드시 JSON 형식으로만 응답할 것

                응답 형식:
                {
                  "category": "장애 유형",
                  "summary": "로그 요약",
                  "possibleCauses": ["원인 후보1", "원인 후보2", "원인 후보3"],
                  "runbook": ["점검 순서1", "점검 순서2", "점검 순서3"],
                  "confidence": 85
                }

                장애 제목: %s
                장애 설명: %s
                태그: %s
                원본 로그:
                %s
                """.formatted(
                title,
                description,
                tags,
                rawLog
        );
    }
}

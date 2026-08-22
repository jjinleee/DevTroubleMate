package com.jinlee.devtroublemate.ai.prompt;

import java.util.List;

public final class TroubleAnalysisPrompt {

    public static final String VERSION = "trouble-analysis-v1";
    public static final String MODEL_NAME = "gpt-4o-mini";

    private TroubleAnalysisPrompt() {
    }

    public static String render(
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
                - 원인 후보와 점검 순서는 각각 최대 3개만 작성할 것
                - 각 원인 후보와 점검 순서는 빈 문자열이 아니어야 함
                - 점검 순서는 실제 개발자가 실행할 수 있는 행동 중심으로 작성할 것
                - confidence는 0부터 100 사이의 정수로 작성할 것
                - 마크다운 코드블록과 추가 설명을 사용하지 말 것
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
                """.formatted(title, description, tags, rawLog);
    }
}

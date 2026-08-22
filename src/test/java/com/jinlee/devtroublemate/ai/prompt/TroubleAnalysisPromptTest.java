package com.jinlee.devtroublemate.ai.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TroubleAnalysisPromptTest {

    @Test
    void renderVersionedPromptWithTroubleData() {
        String prompt = TroubleAnalysisPrompt.render(
                "JWT 오류",
                "로그인 실패",
                "ERROR 401",
                List.of("JWT")
        );

        assertThat(TroubleAnalysisPrompt.VERSION).isEqualTo("trouble-analysis-v1");
        assertThat(TroubleAnalysisPrompt.MODEL_NAME).isEqualTo("gpt-4o-mini");
        assertThat(prompt).contains("JWT 오류", "로그인 실패", "ERROR 401", "JWT");
        assertThat(prompt).contains("각각 최대 3개", "0부터 100 사이");
    }
}

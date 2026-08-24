package com.jinlee.devtroublemate.ai.event;

import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.service.AIAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AIAnalysisEventListenerTest {

    @Mock AIAnalysisService aiAnalysisService;
    @InjectMocks AIAnalysisEventListener listener;

    @Test
    void analyzeAfterTroubleCommit() {
        listener.handle(new AIAnalysisRequestedEvent(1L));

        verify(aiAnalysisService).analyzeStoredTrouble(1L);
    }

    @Test
    void doNotPropagateProviderFailureToCommittedTrouble() {
        doThrow(AIServiceException.timeout(new RuntimeException()))
                .when(aiAnalysisService).analyzeStoredTrouble(1L);

        assertThatCode(() -> listener.handle(new AIAnalysisRequestedEvent(1L)))
                .doesNotThrowAnyException();
    }

    @Test
    void doNotPropagateUnexpectedFailureToCommittedTrouble() {
        doThrow(new IllegalStateException("unexpected"))
                .when(aiAnalysisService).analyzeStoredTrouble(1L);

        assertThatCode(() -> listener.handle(new AIAnalysisRequestedEvent(1L)))
                .doesNotThrowAnyException();
    }
}

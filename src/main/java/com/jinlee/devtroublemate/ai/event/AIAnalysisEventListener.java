package com.jinlee.devtroublemate.ai.event;

import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.service.AIAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AIAnalysisEventListener {

    private final AIAnalysisService aiAnalysisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AIAnalysisRequestedEvent event) {
        try {
            aiAnalysisService.analyzeStoredTrouble(event.troubleId());
        } catch (AIServiceException ignored) {
            // AIAnalysisService가 실패 상태를 저장하므로 원본 트랜잭션에는 영향을 주지 않는다.
        } catch (RuntimeException exception) {
            log.error("커밋 후 AI 분석 처리 중 예상하지 못한 오류가 발생했습니다. troubleId={}",
                    event.troubleId(), exception);
        }
    }
}

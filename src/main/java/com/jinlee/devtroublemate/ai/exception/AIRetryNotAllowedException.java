package com.jinlee.devtroublemate.ai.exception;

public class AIRetryNotAllowedException extends RuntimeException {
    public AIRetryNotAllowedException(Long troubleId) {
        super("대기 또는 실패 상태인 AI 분석만 재시도할 수 있습니다. troubleId=" + troubleId);
    }
}

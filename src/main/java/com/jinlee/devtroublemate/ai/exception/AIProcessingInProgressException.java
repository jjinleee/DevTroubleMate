package com.jinlee.devtroublemate.ai.exception;

public class AIProcessingInProgressException extends RuntimeException {
    public AIProcessingInProgressException(Long troubleId) {
        super("AI 분석이 이미 처리 중입니다. troubleId=" + troubleId);
    }
}

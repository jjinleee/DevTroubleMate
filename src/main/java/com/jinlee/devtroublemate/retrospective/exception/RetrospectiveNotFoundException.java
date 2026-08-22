package com.jinlee.devtroublemate.retrospective.exception;

public class RetrospectiveNotFoundException extends RuntimeException {
    public RetrospectiveNotFoundException(Long troubleId) {
        super("존재하지 않는 장애 회고입니다. troubleId=" + troubleId);
    }
}

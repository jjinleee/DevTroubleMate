package com.jinlee.devtroublemate.retrospective.exception;

public class RetrospectiveAlreadyExistsException extends RuntimeException {
    public RetrospectiveAlreadyExistsException(Long troubleId) {
        super("이미 장애 회고가 존재합니다. troubleId=" + troubleId);
    }
}

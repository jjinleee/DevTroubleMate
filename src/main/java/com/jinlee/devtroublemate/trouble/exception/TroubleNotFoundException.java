package com.jinlee.devtroublemate.trouble.exception;

public class TroubleNotFoundException extends RuntimeException {

    public TroubleNotFoundException() {
        super("존재하지 않는 장애입니다.");
    }

    public TroubleNotFoundException(Long troubleId) {
        super("존재하지 않는 장애입니다. troubleId=" + troubleId);
    }
}

package com.jinlee.devtroublemate.embedding.exception;

public class TroubleEmbeddingNotFoundException extends RuntimeException {
    public TroubleEmbeddingNotFoundException(Long troubleId) {
        super("장애의 Embedding이 존재하지 않습니다. troubleId=" + troubleId);
    }
}

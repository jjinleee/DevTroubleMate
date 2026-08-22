package com.jinlee.devtroublemate.trouble.exception;

public class InvalidTroubleSortException extends RuntimeException {

    public InvalidTroubleSortException(String property) {
        super("허용되지 않은 정렬 필드입니다: " + property);
    }
}

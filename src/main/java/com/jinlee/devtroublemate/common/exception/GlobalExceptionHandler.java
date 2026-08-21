package com.jinlee.devtroublemate.common.exception;

import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TroubleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTroubleNotFoundException(
            TroubleNotFoundException exception
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        ErrorResponse response = ErrorResponse.of(
                status.value(),
                "TROUBLE_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        ErrorResponse response = ErrorResponse.of(
                status.value(),
                "VALIDATION_ERROR",
                message
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}

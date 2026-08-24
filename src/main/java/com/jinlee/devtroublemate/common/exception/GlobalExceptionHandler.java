package com.jinlee.devtroublemate.common.exception;

import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.exception.AIProcessingInProgressException;
import com.jinlee.devtroublemate.ai.exception.AIRetryNotAllowedException;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.exception.InvalidTroubleSortException;
import com.jinlee.devtroublemate.retrospective.exception.RetrospectiveAlreadyExistsException;
import com.jinlee.devtroublemate.retrospective.exception.RetrospectiveNotFoundException;
import com.jinlee.devtroublemate.embedding.exception.TroubleEmbeddingNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ErrorResponse> handleAIServiceException(AIServiceException exception) {
        return ResponseEntity.status(exception.getStatus()).body(ErrorResponse.of(
                exception.getStatus().value(),
                exception.getCode(),
                exception.getMessage()
        ));
    }

    @ExceptionHandler(AIProcessingInProgressException.class)
    public ResponseEntity<ErrorResponse> handleAIProcessingInProgressException(
            AIProcessingInProgressException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "AI_PROCESSING_IN_PROGRESS",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(AIRetryNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleAIRetryNotAllowedException(
            AIRetryNotAllowedException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "AI_RETRY_NOT_ALLOWED",
                exception.getMessage()
        ));
    }

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException() {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_REQUEST_BODY",
                "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해 주세요."
        ));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException() {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ErrorResponse.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "UNSUPPORTED_MEDIA_TYPE",
                "지원하지 않는 Content-Type입니다. application/json을 사용해 주세요."
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                exception.getName() + " 값의 형식이 올바르지 않습니다."
        ));
    }

    @ExceptionHandler(InvalidTroubleSortException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTroubleSortException(
            InvalidTroubleSortException exception
    ) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(TroubleEmbeddingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTroubleEmbeddingNotFoundException(
            TroubleEmbeddingNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "TROUBLE_EMBEDDING_NOT_FOUND",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(RetrospectiveNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRetrospectiveNotFoundException(
            RetrospectiveNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "RETROSPECTIVE_NOT_FOUND",
                exception.getMessage()
        ));
    }

    @ExceptionHandler(RetrospectiveAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleRetrospectiveAlreadyExistsException(
            RetrospectiveAlreadyExistsException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "RETROSPECTIVE_ALREADY_EXISTS",
                exception.getMessage()
        ));
    }
}

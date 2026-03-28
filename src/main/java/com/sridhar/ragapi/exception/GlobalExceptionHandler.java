package com.sridhar.ragapi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PromptInjectionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInjection(Exception ex) {
        // Log the exception (you can use a logger here)
        log.error("Prompt Injection", ex);
        return new ErrorResponse(
                "https://api.smartdocs.io/errors/prompt-injection",
                "Prompt Injection Detected",
                400,
                ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex) {
        // Log the exception (you can use a logger here)
        log.error("Unhandled exception", ex);  // logs full stack trace internally
        return new ErrorResponse(
                "about:blank",
                "Internal Server Error",
                500,
                "An unexpected error occurred. Please try again."
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleEmptyPDF(IllegalArgumentException ex){
        log.info("IllegalArgumentException: {}", ex.getMessage());
        return new ErrorResponse(
                "https://api.smartdocs.io/errors/invalid-pdf",
                "Invalid PDF",
                400,
                ex.getMessage()
        );

    }
}

// Create a new file, e.g., GlobalExceptionHandler.java in a new "exception" package
package com.smartdorm.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 返回 401
                .body(Map.of("error", "Unauthorized", "message", ex.getMessage()));
    }

    // 你还可以添加其他异常处理器，例如处理 UsernameNotFoundException
    // @ExceptionHandler(UsernameNotFoundException.class)
    // public ...
}
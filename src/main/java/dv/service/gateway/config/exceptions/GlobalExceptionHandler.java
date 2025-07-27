package dv.service.gateway.config.exceptions;

import dv.service.gateway.dtos.AppResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
        log.info("Exception: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                AppResponse.error("There was an unexpected error, please contact to administrator.", HttpStatus.INTERNAL_SERVER_ERROR),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<AppResponse<Void>> handleRuntimeException(BadRequestException ex, WebRequest request) {
        log.info("BadRequestException : {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                AppResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UnAuthenticateException.class)
    public ResponseEntity<AppResponse<Void>> handleUnAuthenticateException(UnAuthenticateException ex, WebRequest request) {
        return new ResponseEntity<>(
                AppResponse.error(UnAuthenticateException.MESSAGE, HttpStatus.UNAUTHORIZED),
                HttpStatus.UNAUTHORIZED
        );
    }
}
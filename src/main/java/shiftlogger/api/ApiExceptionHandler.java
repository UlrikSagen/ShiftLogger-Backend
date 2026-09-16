package shiftlogger.api;

import shiftlogger.exception.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(400, e.getMessage()));
    }

    @ExceptionHandler(TimeEntryNotFoundException.class)
    public ResponseEntity<ApiError> notFound(TimeEntryNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(404, e.getMessage()));
    }

    public record ApiError(int status, String message) {}
}
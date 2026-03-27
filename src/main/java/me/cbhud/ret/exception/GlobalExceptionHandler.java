package me.cbhud.ret.exception;

import jakarta.persistence.EntityNotFoundException;
import me.cbhud.ret.dto.response.ApiErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation errors (e.g. @NotBlank, @NotNull) ─────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    // ── Bad request (e.g. missing iic when not manual) ───────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    // ── Not found (e.g. invoice or item ID does not exist) ──────────
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    // ── Duplicate iic race condition ─────────────────────────────────
    @ExceptionHandler(DuplicateInvoiceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateInvoiceException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    // ── DB unique constraint (fallback for concurrent inserts) ───────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT,
                "Data integrity violation — possible duplicate entry.", null);
    }

    // ── Python worker unreachable / error ────────────────────────────
    @ExceptionHandler(WorkerServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkerError(WorkerServiceException ex) {
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), null);
    }

    // ── Catch-all ────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage(), null);
    }

    // ── Helper ───────────────────────────────────────────────────────
    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
                                                           List<String> details) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}

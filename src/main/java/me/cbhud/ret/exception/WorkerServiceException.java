package me.cbhud.ret.exception;

/**
 * Thrown when the Python worker is unreachable or returns an error.
 */
public class WorkerServiceException extends RuntimeException {

    public WorkerServiceException(String message) {
        super(message);
    }

    public WorkerServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

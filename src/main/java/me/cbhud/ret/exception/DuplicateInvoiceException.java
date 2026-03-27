package me.cbhud.ret.exception;

/**
 * Thrown when a duplicate iic is detected via race condition
 * (the "check-first" passed but the DB UNIQUE constraint caught it).
 */
public class DuplicateInvoiceException extends RuntimeException {

    public DuplicateInvoiceException(String iic) {
        super("Invoice with IIC '%s' already exists.".formatted(iic));
    }
}

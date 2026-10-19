package org.datamate.pharmacy.application.exception;

public class InsufficientStockException extends ApplicationException {

    private static final String ERROR_CODE = "pharmacy.insufficient.stock";

    public InsufficientStockException(String medicationName, int requested, int available) {
        super(ERROR_CODE, "Insufficient stock for " + medicationName + ". Requested: " + requested + ", Available: " + available);
    }
}

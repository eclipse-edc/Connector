package org.eclipse.dataspaceconnector.dataloading;

public class ValidationResult {
    public static ValidationResult OK = new ValidationResult(null);
    private final String error;

    private ValidationResult(String error) {
        this.error = error;
    }

    public static ValidationResult error(String error) {
        return new ValidationResult(error);
    }


    public boolean isInvalid() {
        return error != null;
    }

    public String getError() {
        return error;
    }
}

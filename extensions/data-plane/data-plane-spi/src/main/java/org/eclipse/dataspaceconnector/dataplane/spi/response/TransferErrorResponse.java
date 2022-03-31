package org.eclipse.dataspaceconnector.dataplane.spi.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Error response returned when transfer request failed.
 */
public class TransferErrorResponse {
    private final List<String> errors;

    public TransferErrorResponse(@JsonProperty("errors") List<String> errors) {
        this.errors = errors;
    }

    @JsonProperty("errors")
    public List<String> getErrors() {
        return errors;
    }
}
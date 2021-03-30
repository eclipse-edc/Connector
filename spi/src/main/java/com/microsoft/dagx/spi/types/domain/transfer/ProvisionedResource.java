package com.microsoft.dagx.spi.types.domain.transfer;

/**
 * A provisioned resource that supports a data transfer request.
 */
public class ProvisionedResource {
    private String id;
    private String transferProcessId;
    private String resourceDefinitionId;
    private boolean error;
    private String errorMessage;

    public String getId() {
        return id;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    public String getResourceDefinitionId() {
        return resourceDefinitionId;
    }

    public boolean isError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

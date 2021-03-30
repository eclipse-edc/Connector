package com.microsoft.dagx.spi.types.domain.transfer;

/**
 * A resource to be provisioned to support a data transfer.
 */
public abstract class ResourceDefinition {
    private String id;
    private String transferProcessId;

    public String getId() {
        return id;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }
}

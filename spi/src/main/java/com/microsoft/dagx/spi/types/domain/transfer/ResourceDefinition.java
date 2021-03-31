package com.microsoft.dagx.spi.types.domain.transfer;

/**
 * A resource to be provisioned to support a data transfer.
 */
public abstract class ResourceDefinition {
    protected String id;
    protected String transferProcessId;

    public String getId() {
        return id;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    void setTransferProcessId(String transferProcessId) {
        this.transferProcessId = transferProcessId;
    }

    @SuppressWarnings("unchecked")
    public static class Builder<RD extends ResourceDefinition, B extends Builder<RD, B>> {
        protected final RD resourceDefinition;

        public B id(String id) {
            resourceDefinition.id = id;
            return (B) this;
        }

        public RD build() {
            verify();
            return resourceDefinition;
        }

        protected void verify() {
        }

        protected Builder(RD definition) {
            this.resourceDefinition = definition;
        }
    }
}

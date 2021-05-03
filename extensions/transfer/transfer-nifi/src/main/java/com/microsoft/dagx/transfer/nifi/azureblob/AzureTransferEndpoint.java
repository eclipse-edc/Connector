package com.microsoft.dagx.transfer.nifi.azureblob;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.microsoft.dagx.transfer.nifi.NifiTransferEndpoint;

@JsonTypeName("dagx:azuretransferendpoint")
public class AzureTransferEndpoint extends NifiTransferEndpoint {
    public static final String TYPE_AZURE_STORAGE = "AzureStorage";
    @JsonProperty("blobname")
    private String blobName;
    @JsonProperty("container")
    private String containerName;
    @JsonProperty("account")
    private String account;

    private AzureTransferEndpoint(){
        this.setType(TYPE_AZURE_STORAGE);
    }

    public String getBlobName() {
        return blobName;
    }

    public void setBlobName(String blobName) {
        this.blobName = blobName;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public static final class Builder {
        private final AzureTransferEndpoint azureTransferEndpoint;

        private Builder() {
            azureTransferEndpoint = new AzureTransferEndpoint();
        }

        public static Builder anAzureTransferEndpoint() {
            return new Builder();
        }

        public Builder withBlobName(String blobName) {
            azureTransferEndpoint.setBlobName(blobName);
            return this;
        }

        public Builder withContainerName(String containerName) {
            azureTransferEndpoint.setContainerName(containerName);
            return this;
        }

        public Builder withAccount(String account) {
            azureTransferEndpoint.setAccount(account);
            return this;
        }

        public Builder withKey(String key) {
            azureTransferEndpoint.setKey(key);
            return this;
        }

        public AzureTransferEndpoint build() {
            return azureTransferEndpoint;
        }
    }
}

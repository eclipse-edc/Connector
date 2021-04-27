package com.microsoft.dagx.transfer.types.azure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntryPropertyLookup;
import com.microsoft.dagx.spi.types.domain.transfer.DataDestination;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;

@JsonDeserialize(builder = AzureStorageDestination.Builder.class)
@JsonTypeName("dagx:azurestoragedestination")
public class AzureStorageDestination implements DataDestination {

    private String account;
    private String blobname;
    private DestinationSecretToken secretToken;
    private String container;
    private String key;

    private AzureStorageDestination() {
    }

    @Override
    @JsonProperty
    public String getType() {
        return "AzureStorage";
    }

    @Override
    public DestinationSecretToken getSecretToken() {
        return secretToken;
    }

    public String getAccount() {
        return account;
    }

    public String getBlobname() {
        return blobname;
    }

    public String getContainer() {
        return container;
    }

    public String getKey() {
        return key;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final AzureStorageDestination target;

        @JsonCreator
        public static <K extends DataEntryPropertyLookup> AzureStorageDestination.Builder newInstance() {
            return new Builder();
        }


        public Builder account(String account) {
            target.account = account;
            return this;
        }

        public Builder blobname(String blobName) {
            target.blobname = blobName;
            return this;
        }

        public Builder secretToken(DestinationSecretToken token) {
            target.secretToken = token;
            return this;
        }

        private Builder() {
            target = new AzureStorageDestination();
        }

        public AzureStorageDestination build() {
            return target;
        }

        public Builder container(String container) {
            target.container = container;
            return this;
        }

        public Builder key(String key) {
            target.key = key;
            return this;
        }
    }
}

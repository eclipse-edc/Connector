package com.microsoft.dagx.transfer.nifi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.transfer.DataTarget;

@JsonDeserialize(builder = AzureStorageTarget.Builder.class)
public class AzureStorageTarget implements DataTarget {

    private String account;
    private String blobname;
    private String sas;
    private String container;
    private String key;

    private AzureStorageTarget(){}

    public String getType() {
        return "AzureStorage";
    }

    public String getAccount() {
        return account;
    }

    public String getBlobname() {
        return blobname;
    }

    public String getSas() {
        return sas;
    }

    public String getContainer() {
        return container;
    }

    public String getKey() {
        return key;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final AzureStorageTarget target;

        @JsonCreator
        public static <K extends DataEntryExtensions> AzureStorageTarget.Builder newInstance() {
            return new Builder();
        }


        public Builder account(String account) {
            target.account = account;
            return this;
        }

        public Builder blobName(String blobName) {
            target.blobname = blobName;
            return this;
        }

        public Builder token(String token) {
            target.sas = token;
            return this;
        }

        private Builder() {
            target = new AzureStorageTarget();
        }

        public DataTarget build() {
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

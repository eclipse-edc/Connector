package com.microsoft.dagx.transfer.demo.protocols.spi.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 *
 */
@JsonDeserialize(builder = ObjectStorageMessage.Builder.class)
public class ObjectStorageMessage {
    private String containerName;
    private String accessToken;
    private String key;
    private byte[] data;

    public String getContainerName() {
        return containerName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getKey() {
        return key;
    }

    public byte[] getData() {
        return data;
    }

    private ObjectStorageMessage() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ObjectStorageMessage message;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder containerName(String containerName) {
            message.containerName = containerName;
            return this;
        }

        public Builder accessToken(String containerName) {
            message.accessToken = containerName;
            return this;
        }

        public Builder key(String key) {
            message.key = key;
            return this;
        }

        public Builder data(byte[] data) {
            message.data = data;
            return this;
        }

        public ObjectStorageMessage build() {
            Objects.requireNonNull(message.accessToken);
            Objects.requireNonNull(message.containerName);
            Objects.requireNonNull(message.key);
            Objects.requireNonNull(message.data);
            return message;
        }

        private Builder() {
            message = new ObjectStorageMessage();
        }
    }

}

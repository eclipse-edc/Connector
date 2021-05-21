package com.microsoft.dagx.transfer.demo.protocols.spi.stream.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Sent to consumers when data has been published to a destination.
 */
@JsonTypeName("dagx:payloadmessage")
@JsonDeserialize(builder = DataMessage.Builder.class)
public class DataMessage extends PubSubMessage {
    private String destinationName;
    private byte[] payload;

    public String getDestinationName() {
        return destinationName;
    }

    public byte[] getPayload() {
        return payload;
    }

    private DataMessage() {
        super(Protocol.DATA);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private DataMessage message;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder destinationName(String destinationName) {
            message.destinationName = destinationName;
            return this;
        }

        public Builder payload(byte[] payload) {
            message.payload = payload;
            return this;
        }

        public DataMessage build() {
            Objects.requireNonNull(message.destinationName);
            Objects.requireNonNull(message.payload);
            return message;
        }

        private Builder() {
            message = new DataMessage();
        }

    }

}

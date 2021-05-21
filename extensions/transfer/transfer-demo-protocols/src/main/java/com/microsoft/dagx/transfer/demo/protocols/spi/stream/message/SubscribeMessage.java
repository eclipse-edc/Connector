package com.microsoft.dagx.transfer.demo.protocols.spi.stream.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Subscribes to receive data when published to a destination.
 */
@JsonTypeName("dagx:subscribemessage")
@JsonDeserialize(builder = SubscribeMessage.Builder.class)
public class SubscribeMessage extends PubSubMessage {
    private String destinationName;
    private String accessToken;

    public String getDestinationName() {
        return destinationName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    private SubscribeMessage() {
        super(Protocol.SUBSCRIBE);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private SubscribeMessage message;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder destinationName(String destinationName) {
            message.destinationName = destinationName;
            return this;
        }

        public Builder accessToken(String accessToken) {
            message.accessToken = accessToken;
            return this;
        }

        public SubscribeMessage build() {
            Objects.requireNonNull(message.destinationName);
            Objects.requireNonNull(message.accessToken);
            return message;
        }

        private Builder() {
            message = new SubscribeMessage();
        }

    }

}

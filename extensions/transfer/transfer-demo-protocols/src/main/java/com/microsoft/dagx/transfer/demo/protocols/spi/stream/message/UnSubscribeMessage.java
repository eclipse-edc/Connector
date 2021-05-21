package com.microsoft.dagx.transfer.demo.protocols.spi.stream.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Unsubscribes from a destination.
 */
@JsonTypeName("dagx:unsubscribemessage")
@JsonDeserialize(builder = UnSubscribeMessage.Builder.class)
public class UnSubscribeMessage extends PubSubMessage {
    private String destinationName;

    public String getDestinationName() {
        return destinationName;
    }

    private UnSubscribeMessage() {
        super(Protocol.UNSUBSCRIBE);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private UnSubscribeMessage message;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder destinationName(String destinationName) {
            message.destinationName = destinationName;
            return this;
        }

        public UnSubscribeMessage build() {
            Objects.requireNonNull(message.destinationName);
            return message;
        }

        private Builder() {
            message = new UnSubscribeMessage();
        }

    }

}

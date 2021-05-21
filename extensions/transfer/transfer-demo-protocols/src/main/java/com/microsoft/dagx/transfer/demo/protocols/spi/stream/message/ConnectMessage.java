package com.microsoft.dagx.transfer.demo.protocols.spi.stream.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Connects to a destination.
 */
@JsonTypeName("dagx:connectmessage")
@JsonDeserialize(builder = ConnectMessage.Builder.class)
public class ConnectMessage extends PubSubMessage {
    private ConnectMessage() {
        super(Protocol.CONNECT);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ConnectMessage message;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public ConnectMessage build() {
            return message;
        }

        private Builder() {
            message = new ConnectMessage();
        }

    }

}

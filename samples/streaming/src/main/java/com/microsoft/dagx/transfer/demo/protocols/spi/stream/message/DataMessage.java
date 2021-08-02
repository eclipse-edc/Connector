package com.microsoft.dagx.transfer.demo.protocols.spi.stream.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Sent to consumers when data has been published to a topic.
 */
@JsonTypeName("dagx:payloadmessage")
@JsonDeserialize(builder = DataMessage.Builder.class)
public class DataMessage extends PubSubMessage {
    private String topicName;
    private byte[] payload;

    public String getTopicName() {
        return topicName;
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

        public Builder topicName(String topicName) {
            message.topicName = topicName;
            return this;
        }

        public Builder payload(byte[] payload) {
            message.payload = payload;
            return this;
        }

        public DataMessage build() {
            Objects.requireNonNull(message.topicName);
            Objects.requireNonNull(message.payload);
            return message;
        }

        private Builder() {
            message = new DataMessage();
        }

    }

}

package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Subscribes to receive data when published to a topic.
 */
@JsonTypeName("dataspaceconnector:subscribemessage")
@JsonDeserialize(builder = SubscribeMessage.Builder.class)
public class SubscribeMessage extends PubSubMessage {
    private String topicName;
    private String accessToken;

    public String getTopicName() {
        return topicName;
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

        public Builder topicName(String topicName) {
            message.topicName = topicName;
            return this;
        }

        public Builder accessToken(String accessToken) {
            message.accessToken = accessToken;
            return this;
        }

        public SubscribeMessage build() {
            Objects.requireNonNull(message.topicName);
            Objects.requireNonNull(message.accessToken);
            return message;
        }

        private Builder() {
            message = new SubscribeMessage();
        }

    }

}

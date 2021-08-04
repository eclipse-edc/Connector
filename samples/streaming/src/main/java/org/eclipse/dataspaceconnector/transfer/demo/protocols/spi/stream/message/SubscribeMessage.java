/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

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

    private SubscribeMessage() {
        super(Protocol.SUBSCRIBE);
    }

    public String getTopicName() {
        return topicName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final SubscribeMessage message;

        private Builder() {
            message = new SubscribeMessage();
        }

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

    }

}

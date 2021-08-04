/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
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
 * Publishes a payload to a topic.
 */
@JsonTypeName("dataspaceconnector:publishmessage")
@JsonDeserialize(builder = PublishMessage.Builder.class)
public class PublishMessage extends PubSubMessage {
    private String topicName;
    private String accessToken;
    private byte[] payload;

    public String getTopicName() {
        return topicName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public byte[] getPayload() {
        return payload;
    }

    private PublishMessage() {
        super(Protocol.PUBLISH);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private PublishMessage message;

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

        public Builder payload(byte[] payload) {
            message.payload = payload;
            return this;
        }

        public PublishMessage build() {
            Objects.requireNonNull(message.topicName, "destinationName");
            Objects.requireNonNull(message.accessToken, "accessToken");
            return message;
        }

        private Builder() {
            message = new PublishMessage();
        }

    }

}

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
 * Unsubscribes from a topic.
 */
@JsonTypeName("dataspaceconnector:unsubscribemessage")
@JsonDeserialize(builder = UnSubscribeMessage.Builder.class)
public class UnSubscribeMessage extends PubSubMessage {
    private String topicName;

    public String getTopicName() {
        return topicName;
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

        public Builder topicName(String topicName) {
            message.topicName = topicName;
            return this;
        }

        public UnSubscribeMessage build() {
            Objects.requireNonNull(message.topicName);
            return message;
        }

        private Builder() {
            message = new UnSubscribeMessage();
        }

    }

}

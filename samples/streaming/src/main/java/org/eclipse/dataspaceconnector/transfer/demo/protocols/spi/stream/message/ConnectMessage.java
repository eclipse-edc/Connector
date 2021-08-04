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

/**
 * Connects to a destination topic.
 */
@JsonTypeName("dataspaceconnector:connectmessage")
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

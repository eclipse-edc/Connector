/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * A message for suspending an in progress transfer in the data plane
 */
public class DataFlowSuspendMessage {

    public static final String DATA_FLOW_SUSPEND_MESSAGE_SIMPLE_TYPE = "DataFlowSuspendMessage";
    public static final String DATA_FLOW_SUSPEND_MESSAGE_TYPE = EDC_NAMESPACE + DATA_FLOW_SUSPEND_MESSAGE_SIMPLE_TYPE;
    public static final String DATA_FLOW_SUSPEND_MESSAGE_REASON = EDC_NAMESPACE + "reason";

    private String reason;

    private DataFlowSuspendMessage() {

    }

    public String getReason() {
        return reason;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataFlowSuspendMessage message;

        private Builder() {
            this(new DataFlowSuspendMessage());
        }

        private Builder(DataFlowSuspendMessage message) {
            this.message = message;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder reason(String reason) {
            message.reason = reason;
            return this;
        }

        public DataFlowSuspendMessage build() {
            return message;
        }

    }
}

/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

public class DataFlowResumeMessage {
    private String messageId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DspDataAddress dataAddress;

    public String getMessageId() {
        return messageId;
    }

    public DspDataAddress getDataAddress() {
        return dataAddress;
    }

    @Deprecated(since = "0.18.0")
    public String getProcessId() {
        return UUID.randomUUID().toString();
    }

    public static class Builder {

        private final DataFlowResumeMessage instance = new DataFlowResumeMessage();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {

        }

        public DataFlowResumeMessage build() {
            return instance;
        }

        public Builder messageId(String messageId) {
            instance.messageId = messageId;
            return this;
        }

        public Builder dataAddress(DspDataAddress dataAddress) {
            instance.dataAddress = dataAddress;
            return this;
        }
    }
}

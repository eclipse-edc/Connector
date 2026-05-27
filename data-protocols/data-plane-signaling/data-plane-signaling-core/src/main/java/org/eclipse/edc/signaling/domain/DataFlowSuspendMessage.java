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

public class DataFlowSuspendMessage {

    private String messageId;
    private String reason;

    public String getMessageId() {
        return messageId;
    }

    public String getReason() {
        return reason;
    }

    public static class Builder {

        private final DataFlowSuspendMessage instance = new DataFlowSuspendMessage();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {

        }

        public DataFlowSuspendMessage build() {
            return instance;
        }

        public Builder messageId(String messageId) {
            instance.messageId = messageId;
            return this;
        }

        public Builder reason(String reason) {
            instance.reason = reason;
            return this;
        }
    }
}

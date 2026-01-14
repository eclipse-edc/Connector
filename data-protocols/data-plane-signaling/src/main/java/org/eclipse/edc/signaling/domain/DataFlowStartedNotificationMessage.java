/*
 *  Copyright (c) 2025 Think-it GmbH
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

public final class DataFlowStartedNotificationMessage {

    private DspDataAddress dataAddress;

    private DataFlowStartedNotificationMessage() {
    }

    public DspDataAddress getDataAddress() {
        return dataAddress;
    }

    public static class Builder {

        private final DataFlowStartedNotificationMessage instance = new DataFlowStartedNotificationMessage();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {

        }

        public DataFlowStartedNotificationMessage build() {
            return instance;
        }

        public Builder dataAddress(DspDataAddress dataAddress) {
            instance.dataAddress = dataAddress;
            return this;
        }

    }

}

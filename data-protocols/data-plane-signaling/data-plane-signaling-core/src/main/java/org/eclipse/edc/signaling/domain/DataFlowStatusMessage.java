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

public final class DataFlowStatusMessage {

    private DspDataAddress dataAddress;
    private String state;
    private String error;

    private DataFlowStatusMessage() {

    }

    public DspDataAddress getDataAddress() {
        return dataAddress;
    }

    public String getState() {
        return state;
    }

    public String getError() {
        return error;
    }

    public static class Builder {

        private final DataFlowStatusMessage instance = new DataFlowStatusMessage();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {

        }

        public DataFlowStatusMessage build() {
            return instance;
        }


        public Builder dataAddress(DspDataAddress dataAddress) {
            instance.dataAddress = dataAddress;
            return this;
        }

        public Builder state(String state) {
            instance.state = state;
            return this;
        }

        public Builder error(String error) {
            instance.error = error;
            return this;
        }
    }
}

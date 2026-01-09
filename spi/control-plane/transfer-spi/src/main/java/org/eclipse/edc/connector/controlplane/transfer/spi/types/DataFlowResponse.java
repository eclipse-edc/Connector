/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * A Response for {@link DataFlowController#start} and {@link DataFlowController#prepare} operation
 */
public class DataFlowResponse {

    private DataAddress dataAddress;
    private String dataPlaneId;
    private boolean provisioning;

    private DataFlowResponse() {
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public String getDataPlaneId() {
        return dataPlaneId;
    }

    public boolean isProvisioning() {
        return provisioning;
    }

    public static class Builder {

        DataFlowResponse response;

        private Builder() {
            response = new DataFlowResponse();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dataAddress(DataAddress dataAddress) {
            response.dataAddress = dataAddress;
            return this;
        }

        public Builder dataPlaneId(String dataPlaneId) {
            response.dataPlaneId = dataPlaneId;
            return this;
        }

        public Builder provisioning(boolean provisioning) {
            response.provisioning = provisioning;
            return this;
        }

        public DataFlowResponse build() {
            return response;
        }
    }
}

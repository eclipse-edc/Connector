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

package org.eclipse.edc.connector.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * Represent a request of transfer. It contains the detail of the transfer in the field {@link DataRequest}
 * Eventual custom properties that are not relevant to the communication between consumer/provider should
 * go here and not in of the {@link DataRequest}
 */
public class TransferRequest {

    private DataRequest dataRequest;

    private TransferRequest() {

    }

    /**
     * Get the associated {@link DataRequest}
     *
     * @return The data request.
     */
    public DataRequest getDataRequest() {
        return dataRequest;
    }

    public static class Builder {
        private final TransferRequest request;

        private Builder() {
            request = new TransferRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dataRequest(DataRequest dataRequest) {
            request.dataRequest = dataRequest;
            return this;
        }

        public TransferRequest build() {
            Objects.requireNonNull(request.dataRequest, "DataRequest should not be null");
            return request;
        }
    }
}

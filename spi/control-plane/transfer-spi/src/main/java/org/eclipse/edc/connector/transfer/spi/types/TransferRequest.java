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
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represent a request of transfer. It contains the detail of the transfer in the field {@link DataRequest}
 * Eventual custom properties that are not relevant to the communication between consumer/provider should
 * go here and not in of the {@link DataRequest}
 */
public class TransferRequest {

    private List<CallbackAddress> callbackAddresses = new ArrayList<>();

    private Map<String, String> privateProperties = new HashMap<>();
    
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

    /**
     * Get the list of associated {@link CallbackAddress}
     *
     * @return The callbacks
     */
    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    /**
     * Custom private properties that are associated with this transfer request.
     */
    public Map<String, String> getPrivateProperties() {
        return privateProperties;
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

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            request.callbackAddresses = callbackAddresses;
            return this;
        }

        public Builder privateProperties(Map<String, String> privateProperties) {
            request.privateProperties = privateProperties;
            return this;
        }

        public TransferRequest build() {
            Objects.requireNonNull(request.dataRequest, "DataRequest should not be null");
            return request;
        }
    }
}

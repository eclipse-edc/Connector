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

package org.eclipse.edc.connector.contract.spi.types.negotiation;

import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represent a request for contract negotiation on the consumer side. It contains the detail of the request in {@link ContractRequestData}
 */
public class ContractRequest {

    private ContractRequestData requestData;
    private List<CallbackAddress> callbackAddresses = new ArrayList<>();

    public ContractRequestData getRequestData() {
        return requestData;
    }

    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    public static class Builder {
        private final ContractRequest contractRequest;

        private Builder() {
            contractRequest = new ContractRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder requestData(ContractRequestData requestData) {
            contractRequest.requestData = requestData;
            return this;
        }

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            contractRequest.callbackAddresses = callbackAddresses;
            return this;
        }

        public ContractRequest build() {
            Objects.requireNonNull(contractRequest.requestData, "requestData");
            return contractRequest;
        }
    }
}

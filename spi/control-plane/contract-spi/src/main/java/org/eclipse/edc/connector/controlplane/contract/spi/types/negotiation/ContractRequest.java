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

package org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represent a request for contract negotiation on the consumer side.
 */
public class ContractRequest {

    public static final String CONTRACT_REQUEST_TYPE = EDC_NAMESPACE + "ContractRequest";
    public static final String CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS = EDC_NAMESPACE + "counterPartyAddress";
    public static final String PROTOCOL = EDC_NAMESPACE + "protocol";
    public static final String POLICY = EDC_NAMESPACE + "policy";
    public static final String CALLBACK_ADDRESSES = EDC_NAMESPACE + "callbackAddresses";

    private String protocol;
    private String counterPartyAddress;
    private ContractOffer contractOffer;
    private List<CallbackAddress> callbackAddresses = new ArrayList<>();

    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    public String getProviderId() {
        return this.contractOffer.getPolicy().getAssigner();
    }

    public String getProtocol() {
        return protocol;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public ContractOffer getContractOffer() {
        return contractOffer;
    }

    public static class Builder {
        private final ContractRequest contractRequest;

        private Builder() {
            contractRequest = new ContractRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            contractRequest.callbackAddresses = callbackAddresses;
            return this;
        }

        public Builder protocol(String protocol) {
            contractRequest.protocol = protocol;
            return this;
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            contractRequest.counterPartyAddress = counterPartyAddress;
            return this;
        }

        public Builder contractOffer(ContractOffer contractOffer) {
            contractRequest.contractOffer = contractOffer;
            return this;
        }

        public ContractRequest build() {
            Objects.requireNonNull(contractRequest.protocol, "protocol");
            Objects.requireNonNull(contractRequest.counterPartyAddress, "counterPartyAddress");
            Objects.requireNonNull(contractRequest.contractOffer, "contractOffer");
            return contractRequest;
        }
    }
}

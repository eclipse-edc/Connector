/*
 *  Copyright (c) 2023 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Negotiation API enhancement
 *
 */

package org.eclipse.edc.connector.api.management.contractnegotiation.v3.model;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Represent a request for contract negotiation on the consumer side.
 */
public class ContractRequestDto {

    public static final String CONTRACT_REQUEST_DTO_TYPE = EDC_NAMESPACE + "ContractRequestDto";
    public static final String CONNECTOR_ADDRESS = EDC_NAMESPACE + "connectorAddress";
    public static final String PROTOCOL = EDC_NAMESPACE + "protocol";
    public static final String PROVIDER_ID = EDC_NAMESPACE + "providerId";
    public static final String ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String POLICY = EDC_NAMESPACE + "policy";
    public static final String CALLBACK_ADDRESSES = EDC_NAMESPACE + "callbackAddresses";

    private String providerId;
    private String protocol;
    private String counterPartyAddress;
    private String assetId;
    private String policyId;
    private Policy policy;
    private List<CallbackAddress> callbackAddresses = new ArrayList<>();

    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public Policy getPolicy() {
        return policy;
    }

    public static class Builder {
        private final ContractRequestDto contractRequest;

        private Builder() {
            contractRequest = new ContractRequestDto();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            contractRequest.callbackAddresses = callbackAddresses;
            return this;
        }

        public Builder providerId(String providerId) {
            contractRequest.providerId = providerId;
            return this;
        }

        public Builder assetId(String assetId) {
            contractRequest.assetId = assetId;
            return this;
        }

        public Builder policyId(String policyId) {
            contractRequest.policyId = policyId;
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

        public Builder policy(Policy policy) {
            contractRequest.policy = policy;
            return this;
        }

        public ContractRequestDto build() {
            Objects.requireNonNull(contractRequest.protocol, "protocol");
            Objects.requireNonNull(contractRequest.counterPartyAddress, "counterPartyAddress");

            Objects.requireNonNull(contractRequest.policy, "policy");
            Objects.requireNonNull(contractRequest.assetId, "assetId");

            return contractRequest;
        }
    }
}

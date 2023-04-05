/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.spi.types.negotiation;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Object that wraps the contract offer and provides additional information about e.g. protocol
 * and recipient.
 */
public class ContractOfferRequest implements RemoteMessage {

    private Type type = Type.COUNTER_OFFER;
    private String protocol;
    private String connectorId;
    private String connectorAddress;
    private String correlationId;
    private ContractOffer contractOffer;

    private List<CallbackAddress> callbackAddress = new ArrayList<>();

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Type getType() {
        return type;
    }

    public ContractOffer getContractOffer() {
        return contractOffer;
    }

    public List<CallbackAddress> getCallbackAddress() {
        return callbackAddress;
    }

    public enum Type {
        INITIAL,
        COUNTER_OFFER
    }

    public static class Builder {
        private final ContractOfferRequest contractOfferRequest;

        private Builder() {
            contractOfferRequest = new ContractOfferRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            contractOfferRequest.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            contractOfferRequest.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            contractOfferRequest.connectorAddress = connectorAddress;
            return this;
        }

        public Builder correlationId(String correlationId) {
            contractOfferRequest.correlationId = correlationId;
            return this;
        }

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            contractOfferRequest.callbackAddress = callbackAddresses;
            return this;
        }

        public Builder contractOffer(ContractOffer contractOffer) {
            contractOfferRequest.contractOffer = contractOffer;
            return this;
        }

        public Builder type(Type type) {
            contractOfferRequest.type = type;
            return this;
        }

        public ContractOfferRequest build() {
            Objects.requireNonNull(contractOfferRequest.protocol, "protocol");
            Objects.requireNonNull(contractOfferRequest.connectorId, "connectorId");
            Objects.requireNonNull(contractOfferRequest.connectorAddress, "connectorAddress");
            Objects.requireNonNull(contractOfferRequest.contractOffer, "contractOffer");
            return contractOfferRequest;
        }
    }
}

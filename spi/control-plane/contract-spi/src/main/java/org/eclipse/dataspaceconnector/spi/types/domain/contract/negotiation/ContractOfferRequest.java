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

package org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

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

    public static class Builder {
        private final ContractOfferRequest contractOfferRequest;

        private Builder() {
            this.contractOfferRequest = new ContractOfferRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            this.contractOfferRequest.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            this.contractOfferRequest.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            this.contractOfferRequest.connectorAddress = connectorAddress;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.contractOfferRequest.correlationId = correlationId;
            return this;
        }

        public Builder contractOffer(ContractOffer contractOffer) {
            this.contractOfferRequest.contractOffer = contractOffer;
            return this;
        }

        public Builder type(Type type) {
            this.contractOfferRequest.type = type;
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

    public enum Type {
        INITIAL,
        COUNTER_OFFER
    }
}

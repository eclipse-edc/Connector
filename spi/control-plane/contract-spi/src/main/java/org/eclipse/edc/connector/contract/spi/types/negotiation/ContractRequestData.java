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

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;

import java.util.Objects;

/**
 * Represent the data needed for initiating contract negotiation on the consumer side
 */
public class ContractRequestData {

    private String protocol;
    @Deprecated(forRemoval = true)
    private String connectorId;
    private String counterPartyAddress;
    private ContractOffer contractOffer;
    private String dataSet;

    public String getProtocol() {
        return protocol;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    @Deprecated
    public String getConnectorId() {
        return connectorId;
    }


    public ContractOffer getContractOffer() {
        return contractOffer;
    }

    public String getDataSet() {
        return dataSet;
    }

    public static class Builder {
        private final ContractRequestData contractRequestMessage;

        private Builder() {
            contractRequestMessage = new ContractRequestData();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            contractRequestMessage.protocol = protocol;
            return this;
        }

        @Deprecated
        public Builder connectorId(String connectorId) {
            contractRequestMessage.connectorId = connectorId;
            return this;
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            contractRequestMessage.counterPartyAddress = counterPartyAddress;
            return this;
        }

        public Builder contractOffer(ContractOffer contractOffer) {
            contractRequestMessage.contractOffer = contractOffer;
            return this;
        }

        public Builder dataSet(String dataSet) {
            contractRequestMessage.dataSet = dataSet;
            return this;
        }

        public ContractRequestData build() {
            Objects.requireNonNull(contractRequestMessage.protocol, "protocol");
            Objects.requireNonNull(contractRequestMessage.counterPartyAddress, "callbackAddress");
            Objects.requireNonNull(contractRequestMessage.contractOffer, "contractOffer");
            return contractRequestMessage;
        }
    }
}

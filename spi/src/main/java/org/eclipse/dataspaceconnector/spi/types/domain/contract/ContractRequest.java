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

package org.eclipse.dataspaceconnector.spi.types.domain.contract;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

import java.util.Objects;

public class ContractRequest implements RemoteMessage {

    private String protocol;
    private String connectorId;
    private String connectorAddress;
    private ContractOffer contractOffer;

    @Override
    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getConnectorAddress() {
        return connectorAddress;
    }

    public ContractOffer getContractOffer() {
        return contractOffer;
    }

    public static class Builder {
        private final ContractRequest contractRequest;

        private Builder() {
            this.contractRequest = new ContractRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            this.contractRequest.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            this.contractRequest.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            this.contractRequest.connectorAddress = connectorAddress;
            return this;
        }

        public Builder contractOffer(ContractOffer contractOffer) {
            this.contractRequest.contractOffer = contractOffer;
            return this;
        }

        public ContractRequest build() {
            Objects.requireNonNull(contractRequest.protocol, "protocol");
            Objects.requireNonNull(contractRequest.connectorId, "connectorId");
            Objects.requireNonNull(contractRequest.connectorAddress, "connectorAddress");
            Objects.requireNonNull(contractRequest.contractOffer, "contractOffer");
            return contractRequest;
        }
    }
}

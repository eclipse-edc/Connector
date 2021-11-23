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

import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

import java.util.Objects;

public class AgreementRequest implements RemoteMessage {

    private String protocol;
    private String connectorId;
    private String connectorAddress;
    private ContractAgreement contractAgreement;

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

    public ContractAgreement getContractAgreement() {
        return contractAgreement;
    }

    public static class Builder {
        private final AgreementRequest agreementRequest;

        private Builder() {
            this.agreementRequest = new AgreementRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            this.agreementRequest.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            this.agreementRequest.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            this.agreementRequest.connectorAddress = connectorAddress;
            return this;
        }

        public Builder contractAgreement(ContractAgreement contractAgreement) {
            this.agreementRequest.contractAgreement = contractAgreement;
            return this;
        }

        public AgreementRequest build() {
            Objects.requireNonNull(agreementRequest.protocol, "protocol");
            Objects.requireNonNull(agreementRequest.connectorId, "connectorId");
            Objects.requireNonNull(agreementRequest.connectorAddress, "connectorAddress");
            Objects.requireNonNull(agreementRequest.contractAgreement, "contractAgreement");
            return agreementRequest;
        }
    }
}

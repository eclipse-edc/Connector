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

package org.eclipse.edc.connector.contract.spi.types.agreement;

import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;

import java.util.Objects;

public class ContractAgreementVerificationMessage implements ContractRemoteMessage {

    private String protocol;
    private String connectorAddress;
    private String processId;

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getConnectorAddress() {
        return connectorAddress;
    }

    @Override
    public String getProcessId() {
        return processId;
    }

    public static class Builder {
        private final ContractAgreementVerificationMessage message;

        private Builder() {
            this.message = new ContractAgreementVerificationMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            this.message.protocol = protocol;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            this.message.connectorAddress = connectorAddress;
            return this;
        }

        public Builder processId(String processId) {
            this.message.processId = processId;
            return this;
        }

        public ContractAgreementVerificationMessage build() {
            Objects.requireNonNull(message.protocol, "protocol");
            Objects.requireNonNull(message.connectorAddress, "connectorAddress");
            Objects.requireNonNull(message.processId, "processId");
            return message;
        }
    }
}

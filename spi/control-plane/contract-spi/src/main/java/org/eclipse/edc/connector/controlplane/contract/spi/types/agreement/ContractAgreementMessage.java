/*
 *  Copyright (c) 2021 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.types.agreement;

import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;

import java.util.Objects;

public class ContractAgreementMessage extends ContractRemoteMessage {

    private ContractAgreement contractAgreement;
    private String callbackAddress;

    public ContractAgreement getContractAgreement() {
        return contractAgreement;
    }

    public String getCallbackAddress() {
        return callbackAddress;
    }

    public Policy getPolicy() {
        return contractAgreement.getPolicy();
    }

    public static class Builder extends ProcessRemoteMessage.Builder<ContractAgreementMessage, Builder> {

        private Builder() {
            super(new ContractAgreementMessage());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder contractAgreement(ContractAgreement contractAgreement) {
            this.message.contractAgreement = contractAgreement;
            return this;
        }

        public Builder callbackAddress(String callbackAddress) {
            message.callbackAddress = callbackAddress;
            return this;
        }

        public ContractAgreementMessage build() {
            Objects.requireNonNull(message.contractAgreement, "contractAgreement");
            return super.build();
        }
    }
}

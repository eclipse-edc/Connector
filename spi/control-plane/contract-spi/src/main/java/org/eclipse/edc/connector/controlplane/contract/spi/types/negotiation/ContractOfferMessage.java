/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

package org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;

import static java.util.Objects.requireNonNull;

/**
 * Object that wraps the contract offer and provides additional information about e.g. protocol and recipient.
 * Sent by the provider.
 */
public class ContractOfferMessage extends ContractRemoteMessage {

    private String callbackAddress;
    private ContractOffer contractOffer;

    public String getCallbackAddress() {
        return callbackAddress;
    }

    public ContractOffer getContractOffer() {
        return contractOffer;
    }

    @Override
    public Policy getPolicy() {
        return contractOffer.getPolicy();
    }

    public static class Builder extends ProcessRemoteMessage.Builder<ContractOfferMessage, Builder> {

        private Builder() {
            super(new ContractOfferMessage());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder callbackAddress(String callbackAddress) {
            message.callbackAddress = callbackAddress;
            return this;
        }

        public Builder contractOffer(ContractOffer contractOffer) {
            message.contractOffer = contractOffer;
            return this;
        }

        public ContractOfferMessage build() {
            requireNonNull(message.contractOffer, "contractOffer");
            return super.build();
        }
    }
}

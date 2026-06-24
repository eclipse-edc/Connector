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

package org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Object that wraps the contract offer and provides additional information about e.g. protocol and recipient.
 * Sent by the consumer.
 */
public class ContractRequestMessage extends ContractRemoteMessage {

    private Type type = Type.COUNTER_OFFER;
    private String callbackAddress;

    private ContractOffer contractOffer;

    public Type getType() {
        return type;
    }

    @NotNull
    public ContractOffer getContractOffer() {
        return contractOffer;
    }

    public String getCallbackAddress() {
        return callbackAddress;
    }

    @Override
    public Policy getPolicy() {
        return contractOffer.getPolicy();
    }

    public enum Type {
        INITIAL,
        COUNTER_OFFER
    }

    public static class Builder extends ProcessRemoteMessage.Builder<ContractRequestMessage, Builder> {

        private Builder() {
            super(new ContractRequestMessage());
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

        public Builder type(Type type) {
            message.type = type;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public ContractRequestMessage build() {
            if (message.type == Type.INITIAL) {
                requireNonNull(message.callbackAddress, "callbackAddress");
            }
            requireNonNull(message.contractOffer, "contractOffer");
            return super.build();
        }
    }
}

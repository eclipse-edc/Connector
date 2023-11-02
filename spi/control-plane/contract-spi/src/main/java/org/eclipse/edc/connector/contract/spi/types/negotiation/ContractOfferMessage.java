/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
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

import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

/**
 * Object that wraps the contract offer and provides additional information about e.g. protocol and recipient.
 * Sent by the provider.
 */
public class ContractOfferMessage implements ContractRemoteMessage {

    private String id;
    private String protocol = "unknown";
    private String counterPartyAddress;
    private String callbackAddress;
    private String processId;
    private ContractOffer contractOffer;

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull String getProcessId() {
        return processId;
    }

    @Override
    public void setProtocol(String protocol) {
        Objects.requireNonNull(protocol);
        this.protocol = protocol;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public String getCallbackAddress() {
        return callbackAddress;
    }

    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public ContractOffer getContractOffer() {
        return contractOffer;
    }

    @Override
    public Policy getPolicy() {
        return contractOffer.getPolicy();
    }

    public static class Builder {
        private final ContractOfferMessage message;

        private Builder() {
            message = new ContractOfferMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            message.id = id;
            return this;
        }

        public Builder protocol(String protocol) {
            message.protocol = protocol;
            return this;
        }

        public Builder callbackAddress(String callbackAddress) {
            message.callbackAddress = callbackAddress;
            return this;
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            message.counterPartyAddress = counterPartyAddress;
            return this;
        }

        public Builder processId(String processId) {
            message.processId = processId;
            return this;
        }

        public Builder contractOffer(ContractOffer contractOffer) {
            message.contractOffer = contractOffer;
            return this;
        }

        public ContractOfferMessage build() {
            if (message.id == null) {
                message.id = randomUUID().toString();
            }
            requireNonNull(message.processId, "processId");
            requireNonNull(message.contractOffer, "contractOffer");
            return message;
        }
    }
}

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
import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

/**
 * Object that wraps the contract offer and provides additional information about e.g. protocol and recipient.
 * Sent by the consumer.
 */
public class ContractRequestMessage implements ContractRemoteMessage {

    private String id;
    private Type type = Type.COUNTER_OFFER;
    private String protocol = "unknown";
    private String counterPartyAddress;
    private String callbackAddress;
    private String processId;
    private ContractOffer contractOffer;
    private String contractOfferId;
    private String dataset;

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String protocol) {
        Objects.requireNonNull(protocol);
        this.protocol = protocol;
    }

    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    @Override
    @NotNull
    public String getProcessId() {
        return processId;
    }

    public Type getType() {
        return type;
    }

    @Nullable
    public ContractOffer getContractOffer() {
        return contractOffer;
    }

    @Nullable
    public String getContractOfferId() {
        return contractOfferId;
    }

    public String getDataset() {
        return dataset;
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

    public static class Builder {
        private final ContractRequestMessage message;

        private Builder() {
            message = new ContractRequestMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.message.id = id;
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

        public Builder contractOfferId(String id) {
            message.contractOfferId = id;
            return this;
        }

        public Builder type(Type type) {
            message.type = type;
            return this;
        }

        public Builder dataset(String dataset) {
            message.dataset = dataset;
            return this;
        }

        public ContractRequestMessage build() {
            if (message.id == null) {
                message.id = randomUUID().toString();
            }
            requireNonNull(message.processId, "processId");
            if (message.contractOfferId == null) {
                requireNonNull(message.contractOffer, "contractOffer");
            } else {
                requireNonNull(message.contractOfferId, "contractOfferId");
            }
            return message;
        }
    }
}

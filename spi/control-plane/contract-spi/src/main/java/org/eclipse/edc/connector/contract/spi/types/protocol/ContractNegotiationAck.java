/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.contract.spi.types.protocol;

/**
 * Stripped down version of {@link org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation} used in
 * the protocol communication.
 */
public class ContractNegotiationAck {

    private String providerPid;
    private String consumerPid;
    private String state;

    private ContractNegotiationAck() {
    }

    public String getProviderPid() {
        return providerPid;
    }

    public String getConsumerPid() {
        return consumerPid;
    }

    public String getState() {
        return state;
    }

    public static class Builder {

        private final ContractNegotiationAck ack = new ContractNegotiationAck();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder providerPid(String providerPid) {
            ack.providerPid = providerPid;
            return this;
        }

        public Builder consumerPid(String consumerPid) {
            ack.consumerPid = consumerPid;
            return this;
        }

        public Builder state(String state) {
            ack.state = state;
            return this;
        }

        public ContractNegotiationAck build() {
            return ack;
        }
    }
}

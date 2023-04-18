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

package org.eclipse.edc.connector.contract.spi.event.contractnegotiation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This event is raised when the ContractNegotiation has been verified by consumer.
 */
@JsonDeserialize(builder = ContractNegotiationVerified.Builder.class)
public class ContractNegotiationVerified extends ContractNegotiationEvent {

    private ContractNegotiationVerified() {
    }

    @Override
    public String name() {
        return "contract.negotiation.verified";
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractNegotiationEvent.Builder<ContractNegotiationVerified, Builder> {

        @JsonCreator
        private Builder() {
            super(new ContractNegotiationVerified());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}

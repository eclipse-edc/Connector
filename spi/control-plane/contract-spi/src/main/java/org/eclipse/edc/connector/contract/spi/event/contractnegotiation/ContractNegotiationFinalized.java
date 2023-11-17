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
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;

import java.util.Objects;

/**
 * This event is raised when the ContractNegotiation has been finalized by provider.
 */
@JsonDeserialize(builder = ContractNegotiationFinalized.Builder.class)
public class ContractNegotiationFinalized extends ContractNegotiationEvent {

    protected ContractAgreement contractAgreement;

    private ContractNegotiationFinalized() {
    }


    public ContractAgreement getContractAgreement() {
        return contractAgreement;
    }

    @Override
    public String name() {
        return "contract.negotiation.finalized";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractNegotiationEvent.Builder<ContractNegotiationFinalized, Builder> {

        @JsonCreator
        private Builder() {
            super(new ContractNegotiationFinalized());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder contractAgreement(ContractAgreement contractAgreement) {
            event.contractAgreement = contractAgreement;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public ContractNegotiationFinalized build() {
            Objects.requireNonNull(event.contractAgreement);
            return super.build();
        }
    }
}

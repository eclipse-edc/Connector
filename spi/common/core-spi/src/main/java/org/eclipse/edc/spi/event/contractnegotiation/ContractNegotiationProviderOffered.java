/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - expending Event classes
 *
 */

package org.eclipse.edc.spi.event.contractnegotiation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This event is raised when the ContractNegotiation has been offered.
 */
@JsonDeserialize(builder = ContractNegotiationProviderOffered.Builder.class)
public class ContractNegotiationProviderOffered extends ContractNegotiationEvent {

    private ContractNegotiationProviderOffered() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractNegotiationEvent.Builder<ContractNegotiationProviderOffered, Builder> {

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ContractNegotiationProviderOffered());
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}

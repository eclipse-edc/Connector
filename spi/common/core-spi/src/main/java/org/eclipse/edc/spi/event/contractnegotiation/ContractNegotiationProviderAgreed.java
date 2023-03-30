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

package org.eclipse.edc.spi.event.contractnegotiation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This event is raised when the ContractNegotiation has been agreed by provider.
 */
@JsonDeserialize(builder = ContractNegotiationProviderAgreed.Builder.class)
public class ContractNegotiationProviderAgreed extends ContractNegotiationEvent {

    private ContractNegotiationProviderAgreed() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractNegotiationEvent.Builder<ContractNegotiationProviderAgreed, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        @JsonCreator
        private Builder() {
            super(new ContractNegotiationProviderAgreed());
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}

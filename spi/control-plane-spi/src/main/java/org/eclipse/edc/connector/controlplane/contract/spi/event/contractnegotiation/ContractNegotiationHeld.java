/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This event is raised when the ContractNegotiation has been held by a pending guard and it's waiting for an external
 * interaction.
 */
@JsonDeserialize(builder = ContractNegotiationHeld.Builder.class)
public class ContractNegotiationHeld extends ContractNegotiationEvent {

    private ContractNegotiationHeld() {
    }

    @Override
    public String name() {
        return "contract.negotiation.held";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractNegotiationEvent.Builder<ContractNegotiationHeld, Builder> {

        private Builder() {
            super(new ContractNegotiationHeld());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}

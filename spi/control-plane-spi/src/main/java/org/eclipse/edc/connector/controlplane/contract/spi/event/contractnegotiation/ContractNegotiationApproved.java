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
 * This event is raised when the ContractNegotiation has been manually approved by the provider.
 */
@JsonDeserialize(builder = ContractNegotiationApproved.Builder.class)
public class ContractNegotiationApproved extends ContractNegotiationEvent {

    private ContractNegotiationApproved() {
    }

    @Override
    public String name() {
        return "contract.negotiation.approved";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractNegotiationEvent.Builder<ContractNegotiationApproved, Builder> {

        private Builder() {
            super(new ContractNegotiationApproved());
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

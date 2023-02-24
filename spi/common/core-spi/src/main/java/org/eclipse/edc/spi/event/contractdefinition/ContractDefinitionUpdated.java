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

package org.eclipse.edc.spi.event.contractdefinition;

import java.util.Objects;

/**
 * Describe a ContractDefinition modification.
 */
public class ContractDefinitionUpdated extends ContractDefinitionEvent<ContractDefinitionUpdated.Payload> {

    private ContractDefinitionUpdated() {
    }

    /**
     * This class contains all event specific attributes of a ContractDefinition Updated Event
     */
    public static class Payload extends ContractDefinitionEvent.Payload {
    }

    public static class Builder extends ContractDefinitionEvent.Builder<ContractDefinitionUpdated, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ContractDefinitionUpdated(), new Payload());
        }

        @Override
        public Builder contractDefinitionId(String contractDefinitionId) {
            event.payload.contractDefinitionId = contractDefinitionId;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.contractDefinitionId);
        }
    }

}

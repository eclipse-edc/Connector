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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Describe a ContractDefinition modification.
 */
@JsonDeserialize(builder = ContractDefinitionUpdated.Builder.class)
public class ContractDefinitionUpdated extends ContractDefinitionEvent {

    private ContractDefinitionUpdated() {
    }

    @Override
    public String name() {
        return "contract.definition.updated";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractDefinitionEvent.Builder<ContractDefinitionUpdated, Builder> {

        private Builder() {
            super(new ContractDefinitionUpdated());
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

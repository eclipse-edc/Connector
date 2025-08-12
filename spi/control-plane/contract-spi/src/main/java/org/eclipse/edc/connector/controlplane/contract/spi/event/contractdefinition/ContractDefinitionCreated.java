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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - expending Event classes
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.event.contractdefinition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Describe a new ContractDefinition creation, after this has emitted, a ContractDefinition with a certain id will be available.
 */
@JsonDeserialize(builder = ContractDefinitionCreated.Builder.class)
public class ContractDefinitionCreated extends ContractDefinitionEvent {

    private ContractDefinitionCreated() {
    }

    @Override
    public String name() {
        return "contract.definition.created";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractDefinitionEvent.Builder<ContractDefinitionCreated, Builder> {

        private Builder() {
            super(new ContractDefinitionCreated());
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

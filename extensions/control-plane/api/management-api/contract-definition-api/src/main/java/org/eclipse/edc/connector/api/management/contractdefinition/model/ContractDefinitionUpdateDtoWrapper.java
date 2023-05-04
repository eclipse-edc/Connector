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

package org.eclipse.edc.connector.api.management.contractdefinition.model;


import java.util.Objects;

/**
 * Simple DTO wrapper that contains the updating values from the Request {@link ContractDefinitionRequestDto} and the id
 * of the Contract Definition to update.
 * It will be used in the {@link org.eclipse.edc.connector.api.management.contractdefinition.transform.ContractDefinitionUpdateDtoWrapperToContractDefinitionTransformer} for
 * building the {@link org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition} to update.
 */
public class ContractDefinitionUpdateDtoWrapper {

    private ContractDefinitionRequestDto contractDefinition;
    private String id;

    private ContractDefinitionUpdateDtoWrapper() {
    }

    public String getId() {
        return id;
    }

    public ContractDefinitionRequestDto getContractDefinition() {
        return contractDefinition;
    }


    public static final class Builder {
        private final ContractDefinitionUpdateDtoWrapper wrapper;

        private Builder() {
            wrapper = new ContractDefinitionUpdateDtoWrapper();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            wrapper.id = id;
            return this;
        }

        public Builder contractDefinition(ContractDefinitionRequestDto contractDefinition) {
            wrapper.contractDefinition = contractDefinition;
            return this;
        }


        public ContractDefinitionUpdateDtoWrapper build() {
            Objects.requireNonNull(wrapper.id);
            Objects.requireNonNull(wrapper.contractDefinition);
            return wrapper;
        }
    }
}

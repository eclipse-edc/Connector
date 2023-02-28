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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ContractDefinitionUpdateDto.Builder.class)
public class ContractDefinitionUpdateDto extends ContractDefinitionRequestDto {


    private ContractDefinitionUpdateDto() {
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends ContractDefinitionRequestDto.Builder<ContractDefinitionUpdateDto, Builder> {

        private Builder() {
            super(new ContractDefinitionUpdateDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }


        @Override
        public Builder self() {
            return this;
        }

        @Override
        public ContractDefinitionUpdateDto build() {
            return dto;
        }
    }
}

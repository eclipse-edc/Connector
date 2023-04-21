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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.connector.api.management.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = PolicyDefinitionNewUpdateDto.Builder.class)
public class PolicyDefinitionNewUpdateDto extends PolicyDefinitionNewDto {

    private PolicyDefinitionNewUpdateDto() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends PolicyDefinitionNewDto.Builder<PolicyDefinitionNewUpdateDto, Builder> {

        private Builder() {
            super(new PolicyDefinitionNewUpdateDto());
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
        public PolicyDefinitionNewUpdateDto build() {
            return dto;
        }
    }
}

/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.connector.api.management.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.policy.model.Policy;

@JsonDeserialize(builder = PolicyDefinitionUpdateDto.Builder.class)
public class PolicyDefinitionUpdateDto extends PolicyDefinitionDto{

    @NotNull
    private Policy policy;

    private PolicyDefinitionUpdateDto() {
    }

    public Policy getPolicy() {
        return policy;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends PolicyDefinitionDto.Builder<PolicyDefinitionUpdateDto, Builder>{

        private Builder() {
            super(new PolicyDefinitionUpdateDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public PolicyDefinitionUpdateDto.Builder policy(Policy policy) {
            dto.policy = policy;
            return this;
        }
        @Override
        public Builder self() {
            return this;
        }

        public PolicyDefinitionUpdateDto build() {
            return dto;
        }
    }
}

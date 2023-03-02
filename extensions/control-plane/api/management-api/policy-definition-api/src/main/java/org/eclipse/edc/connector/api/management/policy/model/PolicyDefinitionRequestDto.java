/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Policy;

import java.util.Objects;

@JsonDeserialize(builder = PolicyDefinitionRequestDto.Builder.class)
public class PolicyDefinitionRequestDto extends PolicyDefinitionDto {

    private String id;

    private PolicyDefinitionRequestDto() {
    }

    public String getId() {
        return id;
    }

    public Policy getPolicy() {
        return policy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Objects.hash(id), policy.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PolicyDefinitionRequestDto that = (PolicyDefinitionRequestDto) o;
        return Objects.equals(id, that.id) && policy.equals(that.policy);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends PolicyDefinitionDto.Builder<PolicyDefinitionRequestDto, PolicyDefinitionRequestDto.Builder> {

        private Builder() {
            super(new PolicyDefinitionRequestDto());
        }

        @JsonCreator
        public static PolicyDefinitionRequestDto.Builder newInstance() {
            return new PolicyDefinitionRequestDto.Builder();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        @Override
        public PolicyDefinitionRequestDto.Builder self() {
            return this;
        }
    }
}

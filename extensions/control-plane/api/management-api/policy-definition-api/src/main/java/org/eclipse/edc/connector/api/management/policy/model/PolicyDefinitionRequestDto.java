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
import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.policy.model.Policy;

import java.util.Objects;

@JsonDeserialize(builder = PolicyDefinitionRequestDto.Builder.class)
public class PolicyDefinitionRequestDto {

    private String id;
    @NotNull
    private Policy policy;

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
    public static final class Builder {

        private final PolicyDefinitionRequestDto dto = new PolicyDefinitionRequestDto();

        private Builder() {

        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder policy(Policy policy) {
            dto.policy = policy;
            return this;
        }

        public PolicyDefinitionRequestDto build() {
            return dto;
        }
    }
}

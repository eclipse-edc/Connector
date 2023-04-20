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
import jakarta.json.JsonObject;

import java.util.Objects;

@JsonDeserialize(builder = PolicyDefinitionNewRequestDto.Builder.class)
public class PolicyDefinitionNewRequestDto extends PolicyDefinitionNewDto {

    private String id;

    private PolicyDefinitionNewRequestDto() {
    }

    public String getId() {
        return id;
    }

    public JsonObject getPolicy() {
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
        PolicyDefinitionNewRequestDto that = (PolicyDefinitionNewRequestDto) o;
        return Objects.equals(id, that.id) && policy.equals(that.policy);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends PolicyDefinitionNewDto.Builder<PolicyDefinitionNewRequestDto, PolicyDefinitionNewRequestDto.Builder> {

        private Builder() {
            super(new PolicyDefinitionNewRequestDto());
        }

        @JsonCreator
        public static PolicyDefinitionNewRequestDto.Builder newInstance() {
            return new PolicyDefinitionNewRequestDto.Builder();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        @Override
        public PolicyDefinitionNewRequestDto.Builder self() {
            return this;
        }

    }
}

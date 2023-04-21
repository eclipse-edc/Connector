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
import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.api.model.BaseResponseDto;

import java.util.Objects;

@JsonDeserialize(builder = PolicyDefinitionNewResponseDto.Builder.class)
public class PolicyDefinitionNewResponseDto extends BaseResponseDto {

    private String id;
    @NotNull
    private JsonObject policy;

    private PolicyDefinitionNewResponseDto() {
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
        PolicyDefinitionNewResponseDto that = (PolicyDefinitionNewResponseDto) o;
        return Objects.equals(id, that.id) && policy.equals(that.policy);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends BaseResponseDto.Builder<PolicyDefinitionNewResponseDto, Builder> {

        private Builder() {
            super(new PolicyDefinitionNewResponseDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder policy(JsonObject policy) {
            dto.policy = policy;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public PolicyDefinitionNewResponseDto build() {
            return dto;
        }
    }
}

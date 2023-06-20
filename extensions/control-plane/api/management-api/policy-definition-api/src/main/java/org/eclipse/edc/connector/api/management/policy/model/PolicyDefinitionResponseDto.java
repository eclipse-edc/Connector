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
import org.eclipse.edc.api.model.BaseResponseDto;
import org.eclipse.edc.policy.model.Policy;

import java.util.Objects;

@JsonDeserialize(builder = PolicyDefinitionResponseDto.Builder.class)
public class PolicyDefinitionResponseDto extends BaseResponseDto {

    private Policy policy;

    private PolicyDefinitionResponseDto() {
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
        var that = (PolicyDefinitionResponseDto) o;
        return Objects.equals(id, that.id) && policy.equals(that.policy);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends BaseResponseDto.Builder<PolicyDefinitionResponseDto, Builder> {

        private Builder() {
            super(new PolicyDefinitionResponseDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder policy(Policy policy) {
            dto.policy = policy;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

    }
}

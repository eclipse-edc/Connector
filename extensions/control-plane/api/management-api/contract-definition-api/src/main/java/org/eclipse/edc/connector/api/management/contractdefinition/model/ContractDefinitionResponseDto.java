/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.api.management.contractdefinition.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.api.model.BaseResponseDto;
import org.eclipse.edc.api.model.CriterionDto;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(builder = ContractDefinitionResponseDto.Builder.class)
public class ContractDefinitionResponseDto extends BaseResponseDto {
    private String accessPolicyId;
    private String contractPolicyId;
    private List<CriterionDto> selectorExpression = new ArrayList<>();

    private ContractDefinitionResponseDto() {
    }

    public String getAccessPolicyId() {
        return accessPolicyId;
    }

    public String getContractPolicyId() {
        return contractPolicyId;
    }

    public List<CriterionDto> getSelectorExpression() {
        return selectorExpression;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends BaseResponseDto.Builder<ContractDefinitionResponseDto, Builder> {
        private Builder() {
            super(new ContractDefinitionResponseDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder accessPolicyId(String accessPolicyId) {
            dto.accessPolicyId = accessPolicyId;
            return this;
        }

        public Builder contractPolicyId(String contractPolicyId) {
            dto.contractPolicyId = contractPolicyId;
            return this;
        }

        public Builder selectorExpression(List<CriterionDto> criteria) {
            dto.selectorExpression = criteria;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}

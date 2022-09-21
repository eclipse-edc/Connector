/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonDeserialize(builder = ContractDefinitionRequestDto.Builder.class)
public class ContractDefinitionRequestDto {

    private String id;
    @NotNull(message = "accessPolicyId cannot be null")
    private String accessPolicyId;
    @NotNull(message = "contractPolicyId cannot be null")
    private String contractPolicyId;
    @Valid
    @NotNull(message = "criteria cannot be null")
    private List<CriterionDto> criteria = new ArrayList<>();

    private ContractDefinitionRequestDto() {
    }

    @AssertTrue(message = "id must be either be null or not blank, and it cannot contain the ':' character")
    @JsonIgnore
    public boolean isIdValid() {
        return Optional.of(this)
                .map(it -> it.id)
                .map(it -> !it.isBlank() && !it.contains(":"))
                .orElse(true);
    }

    public String getAccessPolicyId() {
        return accessPolicyId;
    }

    public String getContractPolicyId() {
        return contractPolicyId;
    }

    public List<CriterionDto> getCriteria() {
        return criteria;
    }

    public String getId() {
        return id;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final ContractDefinitionRequestDto dto;

        private Builder() {
            this.dto = new ContractDefinitionRequestDto();
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

        public Builder criteria(List<CriterionDto> criteria) {
            dto.criteria = criteria;
            return this;
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public ContractDefinitionRequestDto build() {
            return dto;
        }
    }
}

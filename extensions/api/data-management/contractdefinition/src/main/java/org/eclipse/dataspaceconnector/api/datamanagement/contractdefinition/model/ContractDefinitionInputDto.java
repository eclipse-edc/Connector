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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.eclipse.dataspaceconnector.spi.query.Criterion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonDeserialize(builder = ContractDefinitionInputDto.Builder.class)
public class ContractDefinitionInputDto {

    private String id;
    @NotNull(message = "accessPolicyId cannot be null")
    private String accessPolicyId;
    @NotNull(message = "accessPolicyId cannot be null")
    private String contractPolicyId;
    @NotNull(message = "criteria cannot be null")
    private List<Criterion> criteria = new ArrayList<>();

    private ContractDefinitionInputDto() {
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

    public List<Criterion> getCriteria() {
        return criteria;
    }

    public String getId() {
        return id;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final ContractDefinitionInputDto dto;

        private Builder() {
            this.dto = new ContractDefinitionInputDto();
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

        public Builder criteria(List<Criterion> criteria) {
            dto.criteria = criteria;
            return this;
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public ContractDefinitionInputDto build() {
            return dto;
        }
    }
}

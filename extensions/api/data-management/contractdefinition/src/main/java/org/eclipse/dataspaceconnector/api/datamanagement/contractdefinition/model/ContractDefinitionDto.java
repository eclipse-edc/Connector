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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.query.Criterion;

import java.util.List;

@JsonDeserialize(builder = ContractDefinitionDto.Builder.class)
public class ContractDefinitionDto {
    private String accessPolicyId;
    private String contractPolicyId;
    private List<Criterion> criteria;
    private String id;

    private ContractDefinitionDto() {
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
        private final ContractDefinitionDto dto;

        private Builder() {
            dto = new ContractDefinitionDto();
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

        public ContractDefinitionDto build() {
            return dto;
        }
    }
}

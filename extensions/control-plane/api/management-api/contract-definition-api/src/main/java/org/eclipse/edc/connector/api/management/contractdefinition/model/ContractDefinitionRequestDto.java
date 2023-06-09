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

package org.eclipse.edc.connector.api.management.contractdefinition.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.api.model.BaseDto;
import org.eclipse.edc.api.model.CriterionDto;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class ContractDefinitionRequestDto extends BaseDto {

    // constants for JSON-LD transformation
    public static final String CONTRACT_DEFINITION_TYPE = EDC_NAMESPACE + "ContractDefinition";
    public static final String CONTRACT_DEFINITION_ACCESSPOLICY_ID = EDC_NAMESPACE + "accessPolicyId";
    public static final String CONTRACT_DEFINITION_CONTRACTPOLICY_ID = EDC_NAMESPACE + "contractPolicyId";
    public static final String CONTRACT_DEFINITION_ASSETS_SELECTOR = EDC_NAMESPACE + "assetsSelector";
    /**
     * Default validity is set to one year.
     */
    protected String accessPolicyId;
    protected String contractPolicyId;
    protected List<CriterionDto> assetsSelector = new ArrayList<>();

    //this cannot be non-null, because that would break backwards compatibility with the old API
    @JsonProperty(value = ID)
    protected String id;

    protected ContractDefinitionRequestDto() {
    }

    public String getAccessPolicyId() {
        return accessPolicyId;
    }

    public String getContractPolicyId() {
        return contractPolicyId;
    }

    public List<CriterionDto> getAssetsSelector() {
        return assetsSelector;
    }

    public String getId() {
        return id;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        protected final ContractDefinitionRequestDto dto;

        protected Builder() {
            this.dto = new ContractDefinitionRequestDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder accessPolicyId(String accessPolicyId) {
            dto.accessPolicyId = accessPolicyId;
            return self();
        }

        public Builder contractPolicyId(String contractPolicyId) {
            dto.contractPolicyId = contractPolicyId;
            return self();
        }

        public Builder assetsSelector(List<CriterionDto> criteria) {
            dto.assetsSelector = criteria;
            return self();
        }

        public Builder id(String id) {
            dto.id = id;
            return self();
        }

        public Builder self() {
            return this;
        }

        public ContractDefinitionRequestDto build() {
            return dto;
        }

    }
}

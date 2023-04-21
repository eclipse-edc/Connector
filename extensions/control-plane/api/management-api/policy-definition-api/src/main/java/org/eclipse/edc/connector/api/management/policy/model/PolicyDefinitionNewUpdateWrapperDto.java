/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.connector.api.management.policy.model;

public class PolicyDefinitionNewUpdateWrapperDto {

    private PolicyDefinitionNewUpdateDto policyDefinitionUpdateDto;
    private String policyDefinitionId;

    private PolicyDefinitionNewUpdateWrapperDto() {
    }

    public PolicyDefinitionNewUpdateDto getUpdateDto() {
        return policyDefinitionUpdateDto;
    }

    public String getPolicyDefinitionId() {
        return policyDefinitionId;
    }

    public static final class Builder {

        private final PolicyDefinitionNewUpdateWrapperDto dto;

        private Builder() {
            dto = new PolicyDefinitionNewUpdateWrapperDto();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder updateRequest(PolicyDefinitionNewUpdateDto dto) {
            this.dto.policyDefinitionUpdateDto = dto;
            return this;
        }

        public Builder policyDefinitionId(String policyId) {
            this.dto.policyDefinitionId = policyId;
            return this;
        }

        public PolicyDefinitionNewUpdateWrapperDto build() {
            return dto;
        }
    }
}

/*
 *  Copyright (c) 2023 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.connector.api.management.policy.model;

public class PolicyDefinitionUpdateWrapperDto {

    private PolicyDefinitionUpdateDto policyDefinitionUpdateDto;
    private String policyId;

    private PolicyDefinitionUpdateWrapperDto() {
    }

    public PolicyDefinitionUpdateDto getUpdateDto() {
        return policyDefinitionUpdateDto;
    }

    public String getPolicyId() {
        return policyId;
    }

    public static final class Builder {

        private final PolicyDefinitionUpdateWrapperDto dto;

        private Builder() {
            dto = new PolicyDefinitionUpdateWrapperDto();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder updateRequest(PolicyDefinitionUpdateDto dto) {
            this.dto.policyDefinitionUpdateDto = dto;
            return this;
        }

        public Builder policyId(String policyId) {
            this.dto.policyId = policyId;
            return this;
        }

        public PolicyDefinitionUpdateWrapperDto build() {
            return dto;
        }
    }
}

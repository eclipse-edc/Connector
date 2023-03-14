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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.edc.api.model.CriterionDto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class ContractDefinitionRequestDto {

    /**
     * Default validity is set to one year.
     */
    private static final long DEFAULT_VALIDITY = TimeUnit.DAYS.toSeconds(365);

    @NotNull(message = "accessPolicyId cannot be null")
    protected String accessPolicyId;
    @NotNull(message = "contractPolicyId cannot be null")
    protected String contractPolicyId;
    @Valid
    @NotNull(message = "criteria cannot be null")
    protected List<CriterionDto> criteria = new ArrayList<>();
    @Positive(message = "validity must be positive")
    protected long validity = DEFAULT_VALIDITY;


    public String getAccessPolicyId() {
        return accessPolicyId;
    }

    public String getContractPolicyId() {
        return contractPolicyId;
    }

    public List<CriterionDto> getCriteria() {
        return criteria;
    }

    public long getValidity() {
        return validity;
    }

    protected abstract static class Builder<A extends ContractDefinitionRequestDto, B extends Builder<A, B>> {
        protected final A dto;

        protected Builder(A dto) {
            this.dto = dto;
        }


        public B accessPolicyId(String accessPolicyId) {
            dto.accessPolicyId = accessPolicyId;
            return self();
        }

        public B contractPolicyId(String contractPolicyId) {
            dto.contractPolicyId = contractPolicyId;
            return self();
        }

        public B criteria(List<CriterionDto> criteria) {
            dto.criteria = criteria;
            return self();
        }

        public B validity(long validity) {
            dto.validity = validity;
            return self();
        }

        public abstract B self();

        public A build() {
            return dto;
        }

    }
}

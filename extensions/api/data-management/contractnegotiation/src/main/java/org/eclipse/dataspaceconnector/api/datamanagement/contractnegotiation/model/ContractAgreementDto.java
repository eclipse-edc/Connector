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
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.dataspaceconnector.policy.model.Policy;

public class ContractAgreementDto {
    @NotNull
    private String id;
    @NotNull
    private String providerAgentId;
    @NotNull
    private String consumerAgentId;
    @Positive
    private long contractSigningDate;
    @Positive
    private long contractStartDate;
    @Positive
    private long contractEndDate;
    @NotNull
    private String assetId;
    @NotNull
    private Policy policy;

    @AssertTrue
    @JsonIgnore
    public boolean isDatesValid() {

        return contractStartDate < contractEndDate &&
                contractSigningDate < contractEndDate;
    }


    public String getId() {
        return id;
    }

    public String getProviderAgentId() {
        return providerAgentId;
    }

    public String getConsumerAgentId() {
        return consumerAgentId;
    }

    public long getContractSigningDate() {
        return contractSigningDate;
    }

    public long getContractStartDate() {
        return contractStartDate;
    }

    public long getContractEndDate() {
        return contractEndDate;
    }

    public String getAssetId() {
        return assetId;
    }

    public Policy getPolicy() {
        return policy;
    }

    public static final class Builder {
        private final ContractAgreementDto agreement;

        private Builder() {
            agreement = new ContractAgreementDto();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            agreement.id = id;
            return this;
        }

        public Builder providerAgentId(String providerAgentId) {
            agreement.providerAgentId = providerAgentId;
            return this;
        }

        public Builder consumerAgentId(String consumerAgentId) {
            agreement.consumerAgentId = consumerAgentId;
            return this;
        }

        public Builder contractSigningDate(long contractSigningDate) {
            agreement.contractSigningDate = contractSigningDate;
            return this;
        }

        public Builder contractStartDate(long contractStartDate) {
            agreement.contractStartDate = contractStartDate;
            return this;
        }

        public Builder contractEndDate(long contractEndDate) {
            agreement.contractEndDate = contractEndDate;
            return this;
        }

        public Builder assetId(String assetId) {
            agreement.assetId = assetId;
            return this;
        }

        public Builder policy(Policy policy) {
            agreement.policy = policy;
            return this;
        }

        public ContractAgreementDto build() {
            return agreement;
        }
    }
}

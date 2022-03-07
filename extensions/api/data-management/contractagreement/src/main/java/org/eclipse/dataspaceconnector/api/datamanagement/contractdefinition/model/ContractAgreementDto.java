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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model;

public class ContractAgreementDto {
    private String id;
    private String providerAgentId;
    private String consumerAgentId;
    private long contractSigningDate;
    private long contractStartDate;
    private long contractEndDate;
    private String assetId;
    private String policyId;

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

    public String getPolicyId() {
        return policyId;
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

        public Builder policyId(String policyId) {
            agreement.policyId = policyId;
            return this;
        }

        public ContractAgreementDto build() {
            return agreement;
        }
    }
}

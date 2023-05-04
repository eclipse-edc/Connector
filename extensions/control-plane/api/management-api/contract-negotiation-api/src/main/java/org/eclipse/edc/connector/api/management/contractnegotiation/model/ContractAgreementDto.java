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

package org.eclipse.edc.connector.api.management.contractnegotiation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.edc.policy.model.Policy;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class ContractAgreementDto {
    public static final String TYPE = EDC_NAMESPACE + "ContractAgreementDto";
    public static final String CONTRACT_AGREEMENT_ASSETID = EDC_NAMESPACE + "assetId";
    public static final String CONTRACT_AGREEMENT_PROVIDER_AGENTID = EDC_NAMESPACE + "providerAgentId";
    public static final String CONTRACT_AGREEMENT_CONSUMER_AGENTID = EDC_NAMESPACE + "consumerAgentId";
    public static final String CONTRACT_AGREEMENT_SIGNING_DATE = EDC_NAMESPACE + "contractSigningDate";
    public static final String CONTRACT_AGREEMENT_START_DATE = EDC_NAMESPACE + "contractStartDate";
    public static final String CONTRACT_AGREEMENT_END_DATE = EDC_NAMESPACE + "contractEndDate";
    public static final String CONTRACT_AGREEMENT_POLICY = EDC_NAMESPACE + "policy";

    @NotNull(message = "id cannot be null")
    private String id;
    @NotNull(message = "providerAgentId cannot be null")
    private String providerAgentId;
    @NotNull(message = "consumerAgentId cannot be null")
    private String consumerAgentId;
    @Positive(message = "contractSigningDate must be greater than 0")
    private long contractSigningDate;
    @Positive(message = "contractStartDate must be greater than 0")
    private long contractStartDate;
    @Positive(message = "contractEndDate must be greater than 0")
    private long contractEndDate;
    @NotNull(message = "assetId cannot be null")
    private String assetId;
    @NotNull(message = "policy cannot be null")
    private Policy policy;

    @AssertTrue(message = "contractStartDate and contractSigningDate must be lower than contractEndDate")
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

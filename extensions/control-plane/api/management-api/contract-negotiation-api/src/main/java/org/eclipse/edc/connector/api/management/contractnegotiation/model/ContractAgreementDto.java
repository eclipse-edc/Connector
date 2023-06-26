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

import org.eclipse.edc.policy.model.Policy;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class ContractAgreementDto {
    public static final String TYPE = EDC_NAMESPACE + "ContractAgreementDto";
    public static final String CONTRACT_AGREEMENT_ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String CONTRACT_AGREEMENT_PROVIDER_ID = EDC_NAMESPACE + "providerId";
    public static final String CONTRACT_AGREEMENT_CONSUMER_ID = EDC_NAMESPACE + "consumerId";
    public static final String CONTRACT_AGREEMENT_SIGNING_DATE = EDC_NAMESPACE + "contractSigningDate";
    public static final String CONTRACT_AGREEMENT_POLICY = EDC_NAMESPACE + "policy";

    private String id;
    private String providerId;
    private String consumerId;
    private long contractSigningDate;
    private String assetId;
    private Policy policy;

    public String getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public long getContractSigningDate() {
        return contractSigningDate;
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

        public Builder providerId(String providerId) {
            agreement.providerId = providerId;
            return this;
        }

        public Builder consumerId(String consumerId) {
            agreement.consumerId = consumerId;
            return this;
        }

        public Builder contractSigningDate(long contractSigningDate) {
            agreement.contractSigningDate = contractSigningDate;
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

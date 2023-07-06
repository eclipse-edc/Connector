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

package org.eclipse.edc.connector.api.management.contractnegotiation.model;

import org.eclipse.edc.policy.model.Policy;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class ContractOfferDescription {

    public static final String CONTRACT_OFFER_DESCRIPTION_TYPE = EDC_NAMESPACE + "ContractOfferDescription";
    public static final String OFFER_ID = EDC_NAMESPACE + "offerId";
    public static final String ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String POLICY = EDC_NAMESPACE + "policy";

    private String offerId;
    private String assetId;
    private Policy policy;

    private ContractOfferDescription() {
    }

    public String getOfferId() {
        return offerId;
    }

    public String getAssetId() {
        return assetId;
    }

    public Policy getPolicy() {
        return policy;
    }

    public static final class Builder {
        private final ContractOfferDescription dto;

        private Builder() {
            dto = new ContractOfferDescription();
        }

        public static ContractOfferDescription.Builder newInstance() {
            return new ContractOfferDescription.Builder();
        }

        public ContractOfferDescription.Builder offerId(String offerId) {
            dto.offerId = offerId;
            return this;
        }

        public ContractOfferDescription.Builder assetId(String assetId) {
            dto.assetId = assetId;
            return this;
        }

        public ContractOfferDescription.Builder policy(Policy policy) {
            dto.policy = policy;
            return this;
        }

        public ContractOfferDescription build() {
            return dto;
        }
    }
}

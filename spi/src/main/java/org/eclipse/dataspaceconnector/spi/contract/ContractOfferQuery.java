/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.contract;

import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;

import java.util.ArrayList;
import java.util.List;

// TODO: add pagination attributes

/**
 * The {@link ContractOfferFrameworkQuery} narrows down the number of
 * queried {@link org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer}.
 */
public class ContractOfferQuery {

    private VerificationResult verificationResult;
    private List<String> targetAssetIds;

    private ContractOfferQuery() {
    }

    /**
     * Tell the query to filter out all {@link org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer} that are not intended for the connector, the {@link VerificationResult} was created for.
     *
     * @return verification result of the requesting connector
     */
    public VerificationResult getVerificationResult() {
        return verificationResult;
    }

    public static ContractOfferQuery.Builder builder() {
        return ContractOfferQuery.Builder.newInstance();
    }

    /**
     * Tell the query to filter out all {@link org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer} that don't contain at least one of the target assets.
     * An empty list applies no filter.
     *
     * @return list of target assets
     */
    public List<String> getTargetAssetIds() {
        return targetAssetIds;
    }

    public static final class Builder {
        private VerificationResult verificationResult;
        private List<String> assets;

        private Builder() {
            assets = new ArrayList<>();
        }

        public static Builder newInstance() {
            return new ContractOfferQuery.Builder();
        }

        public Builder verificationResult(final VerificationResult verificationResult) {
            this.verificationResult = verificationResult;
            return this;
        }

        public Builder targetAsset(String assetId) {
            this.assets.add(assetId);
            return this;
        }

        public Builder targetAssets(List<String> assetIds) {
            this.assets.addAll(assetIds);
            return this;
        }

        public ContractOfferQuery build() {
            final ContractOfferQuery contractOfferQuery = new ContractOfferQuery();
            contractOfferQuery.verificationResult = this.verificationResult;
            contractOfferQuery.targetAssetIds = this.assets;
            return contractOfferQuery;
        }
    }
}
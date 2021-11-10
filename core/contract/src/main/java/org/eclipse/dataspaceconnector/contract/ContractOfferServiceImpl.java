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

package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFrameworkQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQueryResponse;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferTemplate;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ContractOfferServiceImpl implements ContractOfferService {

    private final ContractOfferFramework contractOfferFramework;
    private final AssetIndex assetIndex;

    public ContractOfferServiceImpl(
            ContractOfferFramework contractOfferFramework,
            AssetIndex assetIndex
    ) {
        Objects.requireNonNull(contractOfferFramework, "ContractOfferFramework must not be null!");
        Objects.requireNonNull(assetIndex, "AssetIndex must not be null!");

        this.contractOfferFramework = contractOfferFramework;
        this.assetIndex = assetIndex;
    }

    @Override
    public ContractOfferQueryResponse queryContractOffers(ContractOfferQuery contractOfferQuery) {
        ContractOfferFrameworkQuery contractOfferFrameworkQuery =
                createContractOfferFrameworkQuery(contractOfferQuery);

        Stream<ContractOfferTemplate> contractOfferTemplates = Optional.ofNullable(
                        contractOfferFramework.queryTemplates(contractOfferFrameworkQuery))
                .orElseGet(Stream::empty);

        Predicate<ContractOffer> targetAssetFilter = contractOffer -> contractOfferQuery.getTargetAssetIds().isEmpty() ||
                contractOffer.getAssets().stream().anyMatch(a -> contractOfferQuery.getTargetAssetIds().contains(a.getId()));

        return new ContractOfferQueryResponse(contractOfferTemplates
                .flatMap(this::createContractOfferFromTemplate)
                .filter(targetAssetFilter));
    }

    private Stream<ContractOffer> createContractOfferFromTemplate(
            ContractOfferTemplate contractOfferTemplate) {
        AssetSelectorExpression selectorExpression = contractOfferTemplate.getSelectorExpression();
        if (selectorExpression != null) {
            Stream<Asset> assetStream = assetIndex.queryAssets(selectorExpression);
            return contractOfferTemplate.getTemplatedOffers(assetStream);
        }

        return Stream.empty();
    }

    private ContractOfferFrameworkQuery createContractOfferFrameworkQuery(
            ContractOfferQuery contractOfferQuery) {
        ContractOfferFrameworkQuery.Builder builder = ContractOfferFrameworkQuery.builder();

        Optional.ofNullable(contractOfferQuery.getVerificationResult())
                .ifPresent(builder::verificationResult);

        return builder.build();
    }
}

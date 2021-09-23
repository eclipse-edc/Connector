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
import java.util.stream.Stream;

public class ContractOfferServiceImpl implements ContractOfferService {

    private final ContractOfferFramework contractOfferFramework;
    private final AssetIndex assetIndex;

    public ContractOfferServiceImpl(
            final ContractOfferFramework contractOfferFramework,
            final AssetIndex assetIndex
    ) {
        Objects.requireNonNull(contractOfferFramework, "ContractOfferFramework must not be null!");
        Objects.requireNonNull(assetIndex, "AssetIndex must not be null!");

        this.contractOfferFramework = contractOfferFramework;
        this.assetIndex = assetIndex;
    }

    @Override
    public ContractOfferQueryResponse queryContractOffers(final ContractOfferQuery contractOfferQuery) {
        final ContractOfferFrameworkQuery contractOfferFrameworkQuery =
                createContractOfferFrameworkQuery(contractOfferQuery);

        final Stream<ContractOfferTemplate> contractOfferTemplates = Optional.ofNullable(
                        contractOfferFramework.queryTemplates(contractOfferFrameworkQuery))
                .orElseGet(Stream::empty);

        return new ContractOfferQueryResponse(contractOfferTemplates
                .flatMap(this::createContractOfferFromTemplate));
    }

    private Stream<ContractOffer> createContractOfferFromTemplate(
            final ContractOfferTemplate contractOfferTemplate) {
        final Stream<Asset> assetStream = contractOfferTemplate.getSelectorExpression()
                .map(assetIndex::queryAssets)
                .orElseGet(Stream::empty);

        return contractOfferTemplate.getTemplatedOffers(assetStream);
    }

    private ContractOfferFrameworkQuery createContractOfferFrameworkQuery(
            final ContractOfferQuery contractOfferQuery) {
        final ContractOfferFrameworkQuery.Builder builder = ContractOfferFrameworkQuery.builder();

        Optional.ofNullable(contractOfferQuery.getPrincipal())
                .ifPresent(builder::principal);

        return builder.build();
    }
}

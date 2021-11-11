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
 *       Microsoft Corporation - Refactoring
 *
 */
package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.participant.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

/**
 * Implementation of the {@link ContractOfferService}.
 */
public class ContractOfferServiceImpl implements ContractOfferService {
    private final ContractOfferFramework contractFramework;
    private final AssetIndex assetIndex;

    public ContractOfferServiceImpl(ContractOfferFramework framework, AssetIndex assetIndex) {
        Objects.requireNonNull(framework, "ContractOfferFramework must not be null");
        Objects.requireNonNull(assetIndex, "AssetIndex must not be null");

        this.contractFramework = framework;
        this.assetIndex = assetIndex;
    }

    @Override
    @NotNull
    public Stream<ContractOffer> queryContractOffers(ContractOfferQuery query) {
        var agent = new ParticipantAgent(emptyMap(), emptyMap());
        var definitions = contractFramework.definitionsFor(agent);

        // FIXME the design of ContractOffer#assets(List<Asset> assets) forces all assets from AssetIndex to be materialized in memory; this needs to be fixed
        return definitions.map(definition -> {
            var assets = assetIndex.queryAssets(definition.getAssetSelectorExpression());
            return ContractOffer.Builder.newInstance().policy(definition.getUsagePolicy()).assets(assets.collect(toList())).build();
        });
    }

}

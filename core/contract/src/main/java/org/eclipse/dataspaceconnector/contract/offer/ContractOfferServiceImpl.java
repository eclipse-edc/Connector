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
package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Implementation of the {@link ContractOfferService}.
 */
public class ContractOfferServiceImpl implements ContractOfferService {
    private final ParticipantAgentService agentService;
    private final Supplier<ContractDefinitionService> frameworkSupplier;
    private final AssetIndex assetIndex;

    public ContractOfferServiceImpl(ParticipantAgentService agentService, Supplier<ContractDefinitionService> frameworkSupplier, AssetIndex assetIndex) {
        Objects.requireNonNull(agentService, "ParticipantAgentService must not be null");
        Objects.requireNonNull(frameworkSupplier, "ContractDefinitionService must not be null");
        Objects.requireNonNull(assetIndex, "AssetIndex must not be null");

        this.agentService = agentService;
        this.frameworkSupplier = frameworkSupplier;
        this.assetIndex = assetIndex;
    }

    @Override
    @NotNull
    public Stream<ContractOffer> queryContractOffers(ContractOfferQuery query) {
        var agent = agentService.createFor(query.getClaimToken());
        var definitions = frameworkSupplier.get().definitionsFor(agent);

        return definitions.flatMap(definition -> {
            var assets = assetIndex.queryAssets(definition.getAssetSelectorExpression());
            return assets.map(asset -> ContractOffer.Builder.newInstance().policy(definition.getUsagePolicy()).assets(List.of(asset)).build());
        });
    }

}

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
 *       Fraunhofer Institute for Software and Systems Engineering - extended method implementation
 *
 */

package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Implementation of the {@link ContractOfferService}.
 */
public class ContractOfferServiceImpl implements ContractOfferService {
    private final ParticipantAgentService agentService;
    private final ContractDefinitionService definitionService;
    private final AssetIndex assetIndex;

    public ContractOfferServiceImpl(ParticipantAgentService agentService, ContractDefinitionService definitionService, AssetIndex assetIndex) {
        this.agentService = Objects.requireNonNull(agentService, "ParticipantAgentService must not be null");
        this.definitionService = Objects.requireNonNull(definitionService, "ContractDefinitionService must not be null");
        this.assetIndex = Objects.requireNonNull(assetIndex, "AssetIndex must not be null");
    }

    @Override
    @NotNull
    public Stream<ContractOffer> queryContractOffers(ContractOfferQuery query) {
        var agent = agentService.createFor(query.getClaimToken());
        var definitions = definitionService.definitionsFor(agent);

        return definitions.flatMap(definition -> {
            var assets = assetIndex.queryAssets(definition.getSelectorExpression());
            return assets.map(asset -> ContractOffer.Builder.newInstance()
                    .id(ContractId.createContractId(definition.getId()))
                    .policy(definition.getContractPolicy().withTarget(asset.getId()))
                    .asset(asset)
                    // TODO: this is a workaround for the bug described in https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/753
                    .provider(uri("urn:connector:provider"))
                    .consumer(uri("urn:connector:consumer"))
                    .build());
        });
    }

    /**
     * swallows any exception during uri generation
     */
    private URI uri(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

}

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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of the {@link ContractOfferService}.
 */
public class ContractOfferServiceImpl implements ContractOfferService {
    private final ParticipantAgentService agentService;
    private final ContractDefinitionService definitionService;
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyStore;

    public ContractOfferServiceImpl(ParticipantAgentService agentService, ContractDefinitionService definitionService, AssetIndex assetIndex, PolicyDefinitionStore policyStore) {
        this.agentService = agentService;
        this.definitionService = definitionService;
        this.assetIndex = assetIndex;
        this.policyStore = policyStore;
    }

    @Override
    @NotNull
    public Stream<ContractOffer> queryContractOffers(ContractOfferQuery query) {
        var agent = agentService.createFor(query.getClaimToken());

        return definitionService.definitionsFor(agent)
                .flatMap(definition -> {
                    var assets = assetIndex.queryAssets(definition.getSelectorExpression());
                    return Optional.of(definition.getContractPolicyId())
                            .map(policyStore::findById)
                            .map(policy -> assets.map(asset -> createContractOffer(definition, policy.getPolicy(), asset)))
                            .orElseGet(Stream::empty);
                });
    }

    @NotNull
    private ContractOffer createContractOffer(ContractDefinition definition, Policy policy, Asset asset) {
        return ContractOffer.Builder.newInstance()
                .id(ContractId.createContractId(definition.getId()))
                .policy(policy.withTarget(asset.getId()))
                .asset(asset)
                // TODO: this is a workaround for the bug described in https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/753
                .provider(URI.create("urn:connector:provider"))
                .consumer(URI.create("urn:connector:consumer"))
                .build();
    }
}

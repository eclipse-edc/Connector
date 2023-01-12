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
 *       ZF Friedrichshafen AG - enable asset filtering
 *
 */

package org.eclipse.edc.connector.contract.offer;

import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionService;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

/**
 * Implementation of the {@link ContractOfferResolver}.
 */
public class ContractOfferResolverImpl implements ContractOfferResolver {
    private final ParticipantAgentService agentService;
    private final ContractDefinitionService definitionService;
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyStore;
    private final Clock clock;
    private final Monitor monitor;

    public ContractOfferResolverImpl(ParticipantAgentService agentService, ContractDefinitionService definitionService, AssetIndex assetIndex, PolicyDefinitionStore policyStore, Clock clock, Monitor monitor) {
        this.agentService = agentService;
        this.definitionService = definitionService;
        this.assetIndex = assetIndex;
        this.policyStore = policyStore;
        this.clock = clock;
        this.monitor = monitor;
    }

    @Override
    @NotNull
    public Stream<ContractOffer> queryContractOffers(ContractOfferQuery query) {
        var agent = agentService.createFor(query.getClaimToken());
        var numSeenAssets = new AtomicLong(0);
        var range = query.getRange();
        var limit = range.getTo() - range.getFrom();
        var skip = new AtomicInteger(range.getFrom());

        return definitionService.definitionsFor(agent)
                .takeWhile(d -> numSeenAssets.get() < range.getTo())
                .flatMap(definition -> {
                    var criteria = definition.getSelectorExpression().getCriteria();

                    var querySpecBuilder = QuerySpec.Builder.newInstance()
                            .filter(concat(criteria.stream(), query.getAssetsCriteria().stream()).collect(Collectors.toList()));

                    var querySpec = querySpecBuilder.build();
                    var numAssets = assetIndex.countAssets(querySpec.getFilterExpression());

                    querySpecBuilder.limit((int) numAssets);

                    if (skip.get() > 0) {
                        querySpecBuilder.offset(skip.get());
                    }
                    if (numAssets + numSeenAssets.get() > limit) {
                        querySpecBuilder.limit(limit);
                    }

                    Stream<ContractOffer> offers;
                    if (skip.get() >= numAssets) {
                        offers = Stream.empty();
                    } else {
                        offers = createContractOffers(definition, querySpecBuilder.build())
                                .map(offerBuilder -> offerBuilder
                                        .provider(query.getProvider())
                                        .consumer(query.getConsumer())
                                        .build());
                    }

                    numSeenAssets.addAndGet(numAssets);
                    skip.addAndGet(Long.valueOf(-numAssets).intValue());
                    return offers;
                });
    }

    @NotNull
    private Stream<ContractOffer.Builder> createContractOffers(ContractDefinition definition, QuerySpec assetQuerySpec) {
        return Optional.of(definition.getContractPolicyId())
                .map(policyStore::findById)
                .map(policyDefinition -> assetIndex.queryAssets(assetQuerySpec)
                        .map(asset -> createContractOffer(definition, policyDefinition.getPolicy(), asset)))
                .orElse(Stream.empty());
    }

    @NotNull
    private ContractOffer.Builder createContractOffer(ContractDefinition definition, Policy policy, Asset asset) {

        var contractEndTime = getContractEndtime(definition);

        return ContractOffer.Builder.newInstance()
                .id(ContractId.createContractId(definition.getId()))
                .policy(policy.withTarget(asset.getId()))
                .asset(asset)
                .contractStart(ZonedDateTime.now())
                .contractEnd(contractEndTime);
    }

    @NotNull
    private ZonedDateTime getContractEndtime(ContractDefinition definition) {

        var contractEndTime = Instant.ofEpochMilli(Long.MAX_VALUE).atZone(clock.getZone());
        try {
            contractEndTime = ZonedDateTime.ofInstant(clock.instant().plusSeconds(definition.getValidity()), clock.getZone());
        } catch (ArithmeticException exception) {
            monitor.warning("The added ContractEnd value is bigger than the maximum number allowed by a long value. " +
                    "Changing contractEndTime to Maximum value possible in the ContractOffer");
        }
        return contractEndTime;
    }

}

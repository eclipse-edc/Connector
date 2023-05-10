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
import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver;
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
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

/**
 * Implementation of the {@link ContractOfferResolver}.
 */
public class ContractOfferResolverImpl implements ContractOfferResolver {
    private final String participantId;
    private final ParticipantAgentService agentService;
    private final ContractDefinitionResolver definitionService;
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyStore;
    private final Clock clock;
    private final Monitor monitor;

    public ContractOfferResolverImpl(String participantId,
                                     ParticipantAgentService agentService,
                                     ContractDefinitionResolver definitionService,
                                     AssetIndex assetIndex,
                                     PolicyDefinitionStore policyStore,
                                     Clock clock,
                                     Monitor monitor) {
        this.participantId = participantId;
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

        var numFetchedAssets = new AtomicLong(0);
        var numSeenAssets = new AtomicLong(0);

        var range = query.getRange();
        var offset = Long.valueOf(range.getFrom());
        var limit = Long.valueOf(range.getTo() - range.getFrom());

        return definitionService.definitionsFor(agent)
                .takeWhile(d -> numFetchedAssets.get() < limit)
                .flatMap(definition -> {
                    var criteria = definition.getSelectorExpression().getCriteria();
                    var querySpecBuilder = QuerySpec.Builder.newInstance()
                            .filter(concat(criteria.stream(), query.getAssetsCriteria().stream()).collect(toList()));
                    var querySpec = querySpecBuilder.build();
                    var numAssets = assetIndex.countAssets(querySpec.getFilterExpression());

                    var dynamicOffset = max(0L, offset - numSeenAssets.get());
                    var dynamicLimit = min(limit - numFetchedAssets.get(), max(0, numAssets - dynamicOffset));

                    querySpecBuilder.offset(Long.valueOf(dynamicOffset).intValue());
                    querySpecBuilder.limit(Long.valueOf(dynamicLimit).intValue());

                    var offers = dynamicOffset >= numAssets ? Stream.<ContractOffer>empty() :
                            createContractOffers(definition, querySpecBuilder.build())
                                    .map(offerBuilder -> offerBuilder.providerId(participantId).build());

                    numFetchedAssets.addAndGet(dynamicLimit);
                    numSeenAssets.addAndGet(numAssets);

                    return offers;
                });
    }

    @NotNull
    private Stream<ContractOffer.Builder> createContractOffers(ContractDefinition definition, QuerySpec assetQuerySpec) {
        return Optional.of(definition.getContractPolicyId())
                .map(policyStore::findById)
                .map(policyDefinition -> assetIndex.queryAssets(assetQuerySpec)
                        .map(asset -> createContractOffer(definition, policyDefinition.getPolicy(), asset.getId())))
                .orElse(Stream.empty());
    }

    @NotNull
    private ContractOffer.Builder createContractOffer(ContractDefinition definition, Policy policy, String assetId) {

        var start = clock.instant();
        var zone = clock.getZone();
        var contractEndTime = ZonedDateTime.ofInstant(calculateContractEnd(definition, start), zone);
        var contractStartTime = ZonedDateTime.ofInstant(start, zone);

        return ContractOffer.Builder.newInstance()
                .id(ContractId.createContractId(definition.getId(), assetId))
                .policy(policy.withTarget(assetId))
                .assetId(assetId)
                .contractStart(contractStartTime)
                .contractEnd(contractEndTime);
    }

    @NotNull
    private Instant calculateContractEnd(ContractDefinition definition, Instant start) {

        try {
            return start.plusSeconds(definition.getValidity());
        } catch (ArithmeticException exception) {
            monitor.warning("The added ContractEnd value is bigger than the maximum number allowed by a long value. " +
                    "Changing contractEndTime to Maximum value possible in the ContractOffer");
            return Instant.ofEpochMilli(Long.MAX_VALUE);
        }
    }

}

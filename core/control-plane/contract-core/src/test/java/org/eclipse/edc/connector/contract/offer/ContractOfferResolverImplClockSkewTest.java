/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial Implementation
 *
 */

package org.eclipse.edc.connector.contract.offer;

import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionService;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Note that this test is temporary until contract validation is refactored to use policies.
 */
class ContractOfferResolverImplClockSkewTest {
    private static final int VALIDITY = 10;

    private final Clock clock = new SkewedClock();

    private final ContractDefinitionService contractDefinitionService = mock(ContractDefinitionService.class);
    private final AssetIndex assetIndex = mock(AssetIndex.class);
    private final ParticipantAgentService agentService = mock(ParticipantAgentService.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final Monitor monitor = mock(Monitor.class);

    private ContractOfferResolver contractOfferResolver;

    @BeforeEach
    void setUp() {
        contractOfferResolver = new ContractOfferResolverImpl(agentService, contractDefinitionService, assetIndex, policyStore, clock, monitor);
    }

    /**
     * Verifies that ContractOfferResolverImpl calculates contract validity periods correctly by accounting for time skew and pauses during calculations. Specifically, the
     * contract start and end dates should equal the contract definition validity period and not extend beyond it.
     */
    @Test
    void shouldHandleTimeOffsetsCorrectly() {
        var contractDefinition = getContractDefBuilder().validity(VALIDITY).build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenReturn(Stream.of(contractDefinition));
        var assetStream = Stream.of(Asset.Builder.newInstance().build());
        when(assetIndex.countAssets(anyList())).thenReturn(1L);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(assetStream);
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());
        var query = ContractOfferQuery.builder()
                .claimToken(ClaimToken.Builder.newInstance().build())
                .provider(URI.create("urn:connector:edc-provider"))
                .consumer(URI.create("urn:connector:edc-consumer"))
                .build();

        var offers = contractOfferResolver.queryContractOffers(query);
        assertThat(offers)
                .hasSize(1)
                .allSatisfy(contractOffer -> assertThat(ChronoUnit.SECONDS.between(
                        contractOffer.getContractStart(),
                        contractOffer.getContractEnd()))
                        .isEqualTo(VALIDITY));
    }

    private ContractDefinition.Builder getContractDefBuilder() {
        return ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .validity(TimeUnit.MINUTES.toSeconds(VALIDITY));
    }

    /**
     * A clock that skews time by introducing a random delay between 10000 and 20000ms.
     */
    private static class SkewedClock extends Clock {

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (zone.equals(ZoneOffset.UTC)) {
                return this;
            }
            throw new UnsupportedOperationException("Only UTC is supported");
        }

        @Override
        public Instant instant() {
            return Clock.systemUTC().instant().plusMillis(delay());
        }

        private int delay() {
            return (int) (Math.random() * (20000 - 10000)) + 10000;
        }
    }

}
/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       Fraunhofer Institute for Software and Systems Engineering - add policy engine
 *
 */

package org.eclipse.dataspaceconnector.contract.validation;

import net.datafaker.Faker;
import org.eclipse.dataspaceconnector.contract.policy.PolicyEquality;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static java.time.Instant.EPOCH;
import static java.time.Instant.MAX;
import static java.time.Instant.MIN;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService.NEGOTIATION_SCOPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractValidationServiceImplTest {

    private static final Faker FAKER = new Faker();
    private final Instant now = Instant.now();

    private final ParticipantAgentService agentService = mock(ParticipantAgentService.class);
    private final ContractDefinitionService definitionService = mock(ContractDefinitionService.class);
    private final AssetIndex assetIndex = mock(AssetIndex.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final Clock clock = Clock.fixed(now, UTC);
    private final PolicyEngine policyEngine = mock(PolicyEngine.class);
    private final PolicyEquality policyEquality = mock(PolicyEquality.class);
    private ContractValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        validationService = new ContractValidationServiceImpl(agentService, definitionService, assetIndex, policyStore, clock, policyEngine, policyEquality);
    }

    @Test
    void verifyContractOfferValidation() {
        var originalPolicy = Policy.Builder.newInstance().target("a").build();
        var newPolicy = Policy.Builder.newInstance().target("b").build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionService.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);
        when(policyStore.findById("access")).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());
        when(policyStore.findById("contract")).thenReturn(PolicyDefinition.Builder.newInstance().policy(newPolicy).build());
        when(assetIndex.findById("1")).thenReturn(asset);
        when(policyEngine.evaluate(eq(NEGOTIATION_SCOPE), eq(newPolicy), isA(ParticipantAgent.class))).thenReturn(Result.success(newPolicy));
        when(policyEquality.test(any(), any())).thenReturn(true);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = ContractOffer.Builder.newInstance().id("1:2")
                .asset(asset)
                .policy(originalPolicy)
                .provider(URI.create("provider"))
                .consumer(URI.create("consumer"))
                .build();

        var result = validationService.validate(claimToken, offer);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getPolicy()).isNotSameAs(originalPolicy); // verify the returned policy is the sanitized one
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(definitionService).definitionFor(isA(ParticipantAgent.class), eq("1"));
        verify(assetIndex).findById("1");
        verify(policyEngine).evaluate(eq(NEGOTIATION_SCOPE), eq(newPolicy), isA(ParticipantAgent.class));
    }

    @Test
    void validate_failsIfPolicyNotFound() {
        var originalPolicy = Policy.Builder.newInstance().build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var contractDefinition = getContractDefinition();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionService.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);
        when(policyStore.findById(any())).thenReturn(null);
        when(assetIndex.findById("1")).thenReturn(asset);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = ContractOffer.Builder.newInstance().id("1:2")
                .asset(asset)
                .policy(originalPolicy)
                .provider(URI.create("provider"))
                .consumer(URI.create("consumer"))
                .build();

        var result = validationService.validate(claimToken, offer);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void validate_failsIfOfferedPolicyIsNotTheEqualToTheStoredOne() {
        var offeredPolicy = Policy.Builder.newInstance().permission(Permission.Builder.newInstance().build()).build();
        var storedPolicy = Policy.Builder.newInstance().permission(Permission.Builder.newInstance()
                .constraint(AtomicConstraint.Builder.newInstance().build()).build())
                .build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var contractDefinition = getContractDefinition();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionService.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(storedPolicy).build());
        when(assetIndex.findById("1")).thenReturn(asset);
        when(policyEquality.test(any(), any())).thenReturn(false);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = ContractOffer.Builder.newInstance().id("1:2")
                .asset(asset)
                .policy(offeredPolicy)
                .provider(URI.create("provider"))
                .consumer(URI.create("consumer"))
                .build();

        var result = validationService.validate(claimToken, offer);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void verifyContractAgreementValidation() {
        var newPolicy = Policy.Builder.newInstance().build();
        var contractDefinition = getContractDefinition();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionService.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);
        when(policyStore.findById("access")).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());
        when(policyStore.findById("contract")).thenReturn(PolicyDefinition.Builder.newInstance().policy(newPolicy).build());
        when(policyEngine.evaluate(eq(NEGOTIATION_SCOPE), eq(newPolicy), isA(ParticipantAgent.class))).thenReturn(Result.success(newPolicy));

        var claimToken = ClaimToken.Builder.newInstance().build();
        var agreement = ContractAgreement.Builder.newInstance().id("1")
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString())
                .contractStartDate(now.getEpochSecond())
                .contractEndDate(now.plus(1, ChronoUnit.DAYS).getEpochSecond())
                .contractSigningDate(now.getEpochSecond())
                .id("1:2").build();

        boolean isValid = validationService.validate(claimToken, agreement);

        assertThat(isValid).isTrue();
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(definitionService).definitionFor(isA(ParticipantAgent.class), eq("1"));
        verify(policyEngine).evaluate(eq(NEGOTIATION_SCOPE), eq(newPolicy), isA(ParticipantAgent.class));
    }

    @Test
    void verifyContractAgreementExpired() {
        var past = FAKER.date().between(Timestamp.from(EPOCH), Timestamp.from(now)).toInstant().getEpochSecond();
        var isValid =
                validateAgreementDate(MIN.getEpochSecond(), MIN.getEpochSecond(), past);

        assertThat(isValid).isFalse();
    }

    @Test
    void verifyContractAgreementNotStartedYet() {
        var isValid = validateAgreementDate(MIN.getEpochSecond(), MAX.getEpochSecond(), MAX.getEpochSecond());

        assertThat(isValid).isFalse();
    }

    private boolean validateAgreementDate(long signingDate, long startDate, long endDate) {
        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionService.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(getContractDefinition());
        when(policyEngine.evaluate(eq(NEGOTIATION_SCOPE), isA(Policy.class), isA(ParticipantAgent.class))).thenReturn(Result.success(Policy.Builder.newInstance().build()));

        var claimToken = ClaimToken.Builder.newInstance().build();
        var agreement = ContractAgreement.Builder.newInstance().id("1")
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString())
                .contractSigningDate(signingDate)
                .contractStartDate(startDate)
                .contractEndDate(endDate)
                .id("1:2").build();

        return validationService.validate(claimToken, agreement);
    }

    private ContractDefinition getContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();
    }
}

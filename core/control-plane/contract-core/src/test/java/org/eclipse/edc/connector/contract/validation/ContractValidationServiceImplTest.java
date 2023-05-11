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

package org.eclipse.edc.connector.contract.validation;

import org.eclipse.edc.connector.contract.policy.PolicyEquality;
import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static java.time.Instant.MIN;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.validation.ContractValidationService.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.contract.spi.validation.ContractValidationService.TRANSFER_SCOPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractValidationServiceImplTest {

    private static final String CONSUMER_ID = "consumer";
    private static final String PROVIDER_ID = "provider";

    private final Instant now = Instant.now();

    private final ParticipantAgentService agentService = mock(ParticipantAgentService.class);
    private final ContractDefinitionResolver definitionResolver = mock(ContractDefinitionResolver.class);
    private final AssetIndex assetIndex = mock(AssetIndex.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final Clock clock = Clock.fixed(now, UTC);
    private final PolicyEngine policyEngine = mock(PolicyEngine.class);
    private final PolicyEquality policyEquality = mock(PolicyEquality.class);
    private ContractValidationServiceImpl validationService;

    private static ContractDefinition.Builder createContractDefinitionBuilder() {
        return ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL);
    }

    @BeforeEach
    void setUp() {
        validationService = new ContractValidationServiceImpl(PROVIDER_ID, agentService, definitionResolver, assetIndex, policyStore, policyEngine, policyEquality, clock);
        when(assetIndex.countAssets(anyList())).thenReturn(1L);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void verifyContractOfferValidation(boolean verifyById) {
        var participantAgent = new ParticipantAgent(emptyMap(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));
        var originalPolicy = Policy.Builder.newInstance().target("1").build();
        var newPolicy = Policy.Builder.newInstance().target("1").build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var contractDefinition = createContractDefinition();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(participantAgent);
        when(definitionResolver.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);
        when(policyStore.findById("access")).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());
        when(policyStore.findById("contract")).thenReturn(PolicyDefinition.Builder.newInstance().policy(newPolicy).build());
        when(assetIndex.findById("1")).thenReturn(asset);
        when(policyEngine.evaluate(eq(NEGOTIATION_SCOPE), eq(newPolicy), isA(ParticipantAgent.class))).thenReturn(Result.success(newPolicy));
        when(policyEquality.test(any(), any())).thenReturn(true);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = createContractOffer(asset, originalPolicy);

        var result = verifyById ? validationService.validateInitialOffer(claimToken, offer) : validationService.validateInitialOffer(claimToken, offer.getId());

        assertThat(result.succeeded()).isTrue();
        var validatedOffer = result.getContent().getOffer();
        assertThat(validatedOffer.getPolicy()).isNotSameAs(originalPolicy); // verify the returned policy is the sanitized one
        assertThat(validatedOffer.getAssetId()).isEqualTo(asset.getId());
        assertThat(result.getContent().getConsumerIdentity()).isEqualTo(CONSUMER_ID); // verify the returned policy has the consumer id set, essential for later validation checks
        assertThat(validatedOffer.getProviderId()).isEqualTo(offer.getProviderId());

        verify(agentService).createFor(isA(ClaimToken.class));
        verify(definitionResolver).definitionFor(isA(ParticipantAgent.class), eq("1"));
        verify(assetIndex).findById("1");
        verify(policyEngine).evaluate(eq(NEGOTIATION_SCOPE), eq(newPolicy), isA(ParticipantAgent.class));
    }

    @Test
    void verifyContractOfferValidation_failedIfNoConsumerIdentity() {
        var participantAgent = new ParticipantAgent(emptyMap(), emptyMap());
        var originalPolicy = Policy.Builder.newInstance().target("a").build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var contractDefinition = createContractDefinition();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(participantAgent);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = createContractOffer(asset, originalPolicy);

        var result = validationService.validateInitialOffer(claimToken, offer);

        assertThat(result).isFailed().detail().isEqualTo("Invalid consumer identity");

        verify(agentService).createFor(isA(ClaimToken.class));
    }

    @Test
    void validate_failsIfValidityDiscrepancy() {
        var originalPolicy = Policy.Builder.newInstance().target("a").build();
        var newPolicy = Policy.Builder.newInstance().target("b").build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var contractDefinition = createContractDefinition();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionResolver.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);
        when(policyStore.findById("access")).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());
        when(policyStore.findById("contract")).thenReturn(PolicyDefinition.Builder.newInstance().policy(newPolicy).build());
        when(assetIndex.findById("1")).thenReturn(asset);
        when(policyEngine.evaluate(eq(NEGOTIATION_SCOPE), eq(newPolicy), isA(ParticipantAgent.class))).thenReturn(Result.success(newPolicy));
        when(policyEquality.test(any(), any())).thenReturn(true);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = createContractOffer(asset, originalPolicy);

        var result = validationService.validateInitialOffer(claimToken, offer);

        assertThat(result).isFailed().detail().isEqualTo("Invalid consumer identity");
    }

    @Test
    void validate_failsIfPolicyNotFound() {
        var originalPolicy = Policy.Builder.newInstance().build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var contractDefinition = createContractDefinition();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionResolver.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);
        when(policyStore.findById(any())).thenReturn(null);
        when(assetIndex.findById("1")).thenReturn(asset);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = createContractOffer(asset, originalPolicy);

        var result = validationService.validateInitialOffer(claimToken, offer);

        assertThat(result).isFailed().detail().isEqualTo("Invalid consumer identity");
    }

    @Test
    void validate_failsIfOfferedPolicyIsNotTheEqualToTheStoredOne() {
        var offeredPolicy = Policy.Builder.newInstance().permission(Permission.Builder.newInstance().build()).build();
        var storedPolicy = Policy.Builder.newInstance().permission(Permission.Builder.newInstance()
                        .constraint(AtomicConstraint.Builder.newInstance().build()).build())
                .build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var contractDefinition = createContractDefinition();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionResolver.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(storedPolicy).build());
        when(assetIndex.findById("1")).thenReturn(asset);
        when(policyEquality.test(any(), any())).thenReturn(false);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = createContractOffer(asset, offeredPolicy);

        var result = validationService.validateInitialOffer(claimToken, offer);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void verifyContractAgreementValidation() {
        var newPolicy = Policy.Builder.newInstance().build();
        var participantAgent = new ParticipantAgent(emptyMap(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Class<?>, Object>> captor = ArgumentCaptor.forClass(Map.class);

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(participantAgent);

        when(policyStore.findById("access")).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());
        when(policyStore.findById("contract")).thenReturn(PolicyDefinition.Builder.newInstance().policy(newPolicy).build());
        when(policyEngine.evaluate(eq(TRANSFER_SCOPE), eq(newPolicy), isA(ParticipantAgent.class), any())).thenReturn(Result.success(newPolicy));

        var claimToken = ClaimToken.Builder.newInstance().build();
        var agreement = createContractAgreement()
                .contractSigningDate(now.getEpochSecond())
                .id("1:2:3")
                .consumerId(CONSUMER_ID)
                .build();

        var isValid = validationService.validateAgreement(claimToken, agreement);


        assertThat(isValid.succeeded()).isTrue();

        verify(agentService).createFor(isA(ClaimToken.class));
        verify(policyEngine).evaluate(eq(TRANSFER_SCOPE), eq(newPolicy), isA(ParticipantAgent.class), captor.capture());

        var context = captor.getValue();
        assertThat(context.get(ContractAgreement.class)).isNotNull().isInstanceOf(ContractAgreement.class);

    }

    @ParameterizedTest
    @ValueSource(strings = { "malicious-actor" })
    @NullSource
    void verifyContractAgreementValidation_failedIfInvalidCredentials(String counterPartyId) {
        var newPolicy = Policy.Builder.newInstance().build();
        var participantAgent = new ParticipantAgent(emptyMap(), counterPartyId != null ? Map.of(PARTICIPANT_IDENTITY, counterPartyId) : Map.of());

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(participantAgent);

        when(policyStore.findById("access")).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());
        when(policyStore.findById("contract")).thenReturn(PolicyDefinition.Builder.newInstance().policy(newPolicy).build());

        var claimToken = ClaimToken.Builder.newInstance().build();
        var agreement = createContractAgreement()
                .contractSigningDate(now.getEpochSecond())
                .id("1:2:3")
                .consumerId(CONSUMER_ID)
                .build();

        var isValid = validationService.validateAgreement(claimToken, agreement);

        assertThat(isValid.succeeded()).isFalse();

        verify(agentService).createFor(isA(ClaimToken.class));
    }

    @Test
    void verifyContractAgreementExpired() {
        var past = Instant.now().getEpochSecond() - 5000;
        var isValid = validateAgreementDate(MIN.getEpochSecond());

        assertThat(isValid.failed()).isTrue();
    }

    @Test
    void verifyContractAgreementNotStartedYet() {
        var isValid = validateAgreementDate(MIN.getEpochSecond());

        assertThat(isValid.failed()).isTrue();
    }

    @Test
    void validateAgreement_failsIfIdIsNotValid() {
        var token = ClaimToken.Builder.newInstance().build();
        var agreement = createContractAgreement().id("not a valid ID").build();

        var result = validationService.validateAgreement(token, agreement);

        assertThat(result.failed()).isTrue();

        verify(agentService, times(0)).createFor(eq(token));
    }

    @Test
    void validateConfirmed_succeed() {
        var agreement = createContractAgreement().id("1:2:3").build();
        var offer = createContractOffer();
        var token = ClaimToken.Builder.newInstance().build();

        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, PROVIDER_ID));
        when(agentService.createFor(eq(token))).thenReturn(participantAgent);
        when(policyEquality.test(any(), any())).thenReturn(true);

        var result = validationService.validateConfirmed(token, agreement, offer);

        assertThat(result.succeeded()).isTrue();

        verify(agentService).createFor(isA(ClaimToken.class));
    }

    @Test
    void validateConfirmed_failsIfOfferIsNull() {
        var agreement = createContractAgreement().id("1:2:3").build();
        var token = ClaimToken.Builder.newInstance().build();

        var result = validationService.validateConfirmed(token, agreement, null);

        assertThat(result).isFailed();
        verify(agentService, times(0)).createFor(eq(token));
    }

    @ParameterizedTest
    @ValueSource(strings = { CONSUMER_ID })
    @NullSource
    void validateConfirmed_failsIfInvalidClaims(String counterPartyId) {
        var agreement = createContractAgreement().id("1:2:3").build();
        var offer = createContractOffer();
        var token = ClaimToken.Builder.newInstance().build();

        var participantAgent = new ParticipantAgent(Map.of(), counterPartyId != null ? Map.of(PARTICIPANT_IDENTITY, counterPartyId) : Map.of());
        when(agentService.createFor(eq(token))).thenReturn(participantAgent);

        var result = validationService.validateConfirmed(token, agreement, offer);

        assertThat(result.succeeded()).isFalse();

        verify(agentService).createFor(isA(ClaimToken.class));
    }

    @Test
    void validateConfirmed_failsIfIdIsNotValid() {
        var agreement = createContractAgreement().id("not a valid id").build();
        var offer = createContractOffer();
        var token = ClaimToken.Builder.newInstance().build();

        var result = validationService.validateConfirmed(token, agreement, offer);

        assertThat(result.failed()).isTrue();

        verify(agentService, times(0)).createFor(eq(token));
    }

    @Test
    void validateConfirmed_failsIfPoliciesAreNotEqual() {
        var agreement = createContractAgreement().id("1:2:3").build();
        var offer = createContractOffer();
        var token = ClaimToken.Builder.newInstance().build();

        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));
        when(agentService.createFor(eq(token))).thenReturn(participantAgent);
        when(policyEquality.test(any(), any())).thenReturn(false);

        var result = validationService.validateConfirmed(token, agreement, offer);

        assertThat(result.failed()).isTrue();

        verify(agentService).createFor(eq(token));
    }

    @Test
    void validateConsumerRequest() {
        var token = ClaimToken.Builder.newInstance().build();
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id("1")
                .counterPartyId(CONSUMER_ID)
                .counterPartyAddress("https://consumer.com")
                .protocol("test")
                .build();

        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));
        when(agentService.createFor(eq(token))).thenReturn(participantAgent);

        var result = validationService.validateRequest(token, negotiation);

        assertThat(result.succeeded()).isTrue();

        verify(agentService).createFor(isA(ClaimToken.class));
    }

    @Test
    void validateInitialOffer_assetInOfferNotReferencedByDefinition_shouldFail() {

        var offer = createContractOffer();
        var participantAgent = new ParticipantAgent(emptyMap(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));
        var claimToken = ClaimToken.Builder.newInstance().build();
        var expr = AssetSelectorExpression.Builder.newInstance().constraint(Asset.PROPERTY_ID, "like", "%someAssetId%").build();
        var contractDef = createContractDefinitionBuilder().selectorExpression(expr).build();

        //prepare mocks
        when(agentService.createFor(eq(claimToken))).thenReturn(participantAgent);
        when(definitionResolver.definitionFor(eq(participantAgent), anyString())).thenReturn(contractDef);
        when(assetIndex.findById(anyString())).thenReturn(Asset.Builder.newInstance().build());
        when(assetIndex.countAssets(anyList())).thenReturn(0L);

        //act
        var result = validationService.validateInitialOffer(claimToken, offer);

        //assert
        assertThat(result).isFailed().detail().isEqualTo("Asset ID from the ContractOffer is not included in the ContractDefinition");
    }

    @ParameterizedTest
    @ValueSource(strings = { PROVIDER_ID })
    @NullSource
    void validateConsumerRequest_failsInvalidCredentials(String counterPartyId) {
        var token = ClaimToken.Builder.newInstance().build();
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id("1")
                .counterPartyId(CONSUMER_ID)
                .counterPartyAddress("https://consumer.com")
                .protocol("test")
                .build();

        var participantAgent = new ParticipantAgent(Map.of(), counterPartyId != null ? Map.of(PARTICIPANT_IDENTITY, counterPartyId) : Map.of());
        when(agentService.createFor(eq(token))).thenReturn(participantAgent);

        var result = validationService.validateRequest(token, negotiation);

        assertThat(result.succeeded()).isFalse();

        verify(agentService).createFor(isA(ClaimToken.class));
    }

    private Result<ContractAgreement> validateAgreementDate(long signingDate) {
        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionResolver.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(createContractDefinition());
        when(policyEngine.evaluate(eq(NEGOTIATION_SCOPE), isA(Policy.class), isA(ParticipantAgent.class))).thenReturn(Result.success(Policy.Builder.newInstance().build()));

        var claimToken = ClaimToken.Builder.newInstance().build();
        var agreement = createContractAgreement()
                .id("1:2:3")
                .contractSigningDate(signingDate)
                .build();

        return validationService.validateAgreement(claimToken, agreement);
    }

    private ContractOffer createContractOffer(Asset asset, Policy policy) {
        var now = ZonedDateTime.ofInstant(clock.instant(), clock.getZone());
        return ContractOffer.Builder.newInstance()
                .id(format("1:%s:3", asset.getId()))
                .assetId(asset.getId())
                .policy(policy)
                .providerId(PROVIDER_ID)
                .build();
    }

    @NotNull
    private ContractOffer createContractOffer() {
        return createContractOffer(Asset.Builder.newInstance().build(), Policy.Builder.newInstance().build());
    }

    private ContractDefinition createContractDefinition() {
        return createContractDefinitionBuilder()
                .build();
    }

    private ContractAgreement.Builder createContractAgreement() {
        return ContractAgreement.Builder.newInstance().id("1")
                .providerId(PROVIDER_ID)
                .consumerId(CONSUMER_ID)
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString());
    }
}

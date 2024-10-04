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

package org.eclipse.edc.connector.controlplane.contract.validation;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.policy.PolicyEquality;
import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ValidatableConsumerOffer;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static java.time.Instant.MIN;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractValidationServiceImplTest {

    private static final String CONSUMER_ID = "consumer";
    private static final String PROVIDER_ID = "provider";

    private final Instant now = Instant.now();

    private final AssetIndex assetIndex = mock();
    private final PolicyEngine policyEngine = mock();
    private final PolicyEquality policyEquality = mock();

    private final ContractValidationService validationService =
            new ContractValidationServiceImpl(assetIndex, policyEngine, policyEquality);

    private static ContractDefinition.Builder createContractDefinitionBuilder() {
        return ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract");
    }

    @BeforeEach
    void setUp() {
        when(assetIndex.countAssets(anyList())).thenReturn(1L);
    }

    @Test
    void verifyContractOfferValidation() {
        var participantAgent = new ParticipantAgent(emptyMap(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));
        var originalPolicy = Policy.Builder.newInstance().target("1").build();
        var newPolicy = Policy.Builder.newInstance().target("1").build();
        var asset = Asset.Builder.newInstance().id("1").build();

        when(assetIndex.findById("1")).thenReturn(asset);
        when(policyEngine.evaluate(any(), isA(CatalogPolicyContext.class))).thenReturn(Result.success());
        when(policyEngine.evaluate(any(), isA(ContractNegotiationPolicyContext.class))).thenReturn(Result.success());

        var validatableOffer = createValidatableConsumerOffer(asset, originalPolicy);

        var result = validationService.validateInitialOffer(participantAgent, validatableOffer);

        assertThat(result.succeeded()).isTrue();
        var validatedOffer = result.getContent().getOffer();
        assertThat(validatedOffer.getPolicy()).isNotSameAs(originalPolicy); // verify the returned policy is the sanitized one
        assertThat(validatedOffer.getAssetId()).isEqualTo(asset.getId());
        assertThat(result.getContent().getConsumerIdentity()).isEqualTo(CONSUMER_ID); // verify the returned policy has the consumer id set, essential for later validation checks

        verify(assetIndex).findById("1");
        verify(policyEngine).evaluate(
                eq(newPolicy),
                and(isA(CatalogPolicyContext.class), argThat(c -> c.agent().equals(participantAgent)))
        );
        verify(policyEngine).evaluate(
                eq(newPolicy),
                and(isA(ContractNegotiationPolicyContext.class), argThat(c -> c.agent().equals(participantAgent)))
        );
    }

    @Test
    void verifyContractOfferValidation_failedIfNoConsumerIdentity() {
        var participantAgent = new ParticipantAgent(emptyMap(), emptyMap());
        var originalPolicy = Policy.Builder.newInstance().target("a").build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var validatableOffer = createValidatableConsumerOffer(asset, originalPolicy);

        var result = validationService.validateInitialOffer(participantAgent, validatableOffer);

        assertThat(result).isFailed().detail().isEqualTo("Invalid consumer identity");
    }

    @Test
    void validate_failsIfValidityDiscrepancy() {
        var originalPolicy = Policy.Builder.newInstance().target("a").build();
        var asset = Asset.Builder.newInstance().id("1").build();

        var participantAgent = new ParticipantAgent(emptyMap(), emptyMap());
        when(assetIndex.findById("1")).thenReturn(asset);
        when(policyEquality.test(any(), any())).thenReturn(true);
        var validatableOffer = createValidatableConsumerOffer(asset, originalPolicy);

        var result = validationService.validateInitialOffer(participantAgent, validatableOffer);

        assertThat(result).isFailed().detail().isEqualTo("Invalid consumer identity");
        verifyNoInteractions(policyEngine);
    }

    @Test
    void validate_failsIfOfferedPolicyIsNotTheEqualToTheStoredOne() {
        var offeredPolicy = Policy.Builder.newInstance().permission(Permission.Builder.newInstance().build()).build();
        var asset = Asset.Builder.newInstance().id("1").build();

        var participantAgent = new ParticipantAgent(emptyMap(), emptyMap());
        when(assetIndex.findById("1")).thenReturn(asset);
        when(policyEquality.test(any(), any())).thenReturn(false);

        var validatableOffer = createValidatableConsumerOffer(asset, offeredPolicy);

        var result = validationService.validateInitialOffer(participantAgent, validatableOffer);

        assertThat(result.failed()).isTrue();
        verifyNoInteractions(policyEngine);
    }

    @Test
    void verifyContractAgreementValidation() {
        var newPolicy = Policy.Builder.newInstance().build();
        var participantAgent = new ParticipantAgent(emptyMap(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));

        when(policyEngine.evaluate(any(), any())).thenReturn(Result.success());

        var agreement = createContractAgreement()
                .contractSigningDate(now.getEpochSecond())
                .consumerId(CONSUMER_ID)
                .build();

        var isValid = validationService.validateAgreement(participantAgent, agreement);

        assertThat(isValid.succeeded()).isTrue();

        var captor = ArgumentCaptor.forClass(TransferProcessPolicyContext.class);
        verify(policyEngine).evaluate(eq(newPolicy), captor.capture());
        var context = captor.getValue();
        assertThat(context.contractAgreement()).isNotNull().isInstanceOf(ContractAgreement.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "malicious-actor" })
    @NullSource
    void verifyContractAgreementValidation_failedIfInvalidCredentials(String counterPartyId) {
        var participantAgent = new ParticipantAgent(emptyMap(), counterPartyId != null ? Map.of(PARTICIPANT_IDENTITY, counterPartyId) : Map.of());
        var agreement = createContractAgreement()
                .contractSigningDate(now.getEpochSecond())
                .consumerId(CONSUMER_ID)
                .build();

        var isValid = validationService.validateAgreement(participantAgent, agreement);

        assertThat(isValid.succeeded()).isFalse();

    }

    @Test
    void verifyContractAgreementExpired() {
        var isValid = validateAgreementDate(MIN.getEpochSecond());
        assertThat(isValid.failed()).isTrue();
    }

    @Test
    void verifyContractAgreementNotStartedYet() {
        var isValid = validateAgreementDate(MIN.getEpochSecond());

        assertThat(isValid.failed()).isTrue();
    }

    @Test
    void validateConfirmed_succeed() {
        var agreement = createContractAgreement().id("any").build();
        var offer = createContractOffer();

        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, PROVIDER_ID));
        when(policyEquality.test(any(), any())).thenReturn(true);

        var result = validationService.validateConfirmed(participantAgent, agreement, offer);

        assertThat(result.succeeded()).isTrue();

    }

    @Test
    void validateConfirmed_failsIfOfferIsNull() {
        var agreement = createContractAgreement().id("any").build();
        var participantAgent = new ParticipantAgent(emptyMap(), emptyMap());

        var result = validationService.validateConfirmed(participantAgent, agreement, null);

        assertThat(result).isFailed();
        verifyNoInteractions(policyEquality);
    }

    @Test
    void validateConfirmed_shouldFail_whenParticipantIdentityIsNotTheExpectedOne() {
        var agreement = createContractAgreement().build();
        var offer = createContractOffer();

        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, "not-the-expected-one"));

        var result = validationService.validateConfirmed(participantAgent, agreement, offer);

        assertThat(result).isFailed();
        verifyNoInteractions(policyEquality);
    }

    @Test
    void validateConfirmed_failsIfPoliciesAreNotEqual() {
        var agreement = createContractAgreement().build();
        var offer = createContractOffer();

        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));
        when(policyEquality.test(any(), any())).thenReturn(false);

        var result = validationService.validateConfirmed(participantAgent, agreement, offer);

        assertThat(result.failed()).isTrue();

    }

    @Test
    void validateRequest_shouldReturnSuccess_whenRequestingPartyProvider() {
        var agreement = createContractAgreement().build();
        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, PROVIDER_ID));

        var result = validationService.validateRequest(participantAgent, agreement);

        assertThat(result).isSucceeded();
    }

    @Test
    void validateRequest_shouldReturnSuccess_whenRequestingPartyConsumer() {
        var agreement = createContractAgreement().build();
        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));

        var result = validationService.validateRequest(participantAgent, agreement);

        assertThat(result).isSucceeded();
    }

    @Test
    void validateRequest_shouldReturnFailure_whenRequestingPartyUnauthorized() {
        var agreement = createContractAgreement().build();
        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, "invalid"));

        var result = validationService.validateRequest(participantAgent, agreement);

        assertThat(result).isFailed();
    }

    @Test
    void validateConsumerRequest() {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id("1")
                .counterPartyId(CONSUMER_ID)
                .counterPartyAddress("https://consumer.com")
                .protocol("test")
                .build();

        var participantAgent = new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));

        var result = validationService.validateRequest(participantAgent, negotiation);

        assertThat(result.succeeded()).isTrue();

    }

    @Test
    void validateInitialOffer_assetInOfferNotReferencedByDefinition_shouldFail() {
        var validatableOffer = createValidatableConsumerOffer();
        var participantAgent = new ParticipantAgent(emptyMap(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));

        when(policyEngine.evaluate(any(), isA(CatalogPolicyContext.class))).thenReturn(Result.success());
        when(assetIndex.findById(anyString())).thenReturn(Asset.Builder.newInstance().build());
        when(assetIndex.countAssets(anyList())).thenReturn(0L);

        var result = validationService.validateInitialOffer(participantAgent, validatableOffer);

        assertThat(result).isFailed().detail().isEqualTo("Asset ID from the ContractOffer is not included in the ContractDefinition");
    }

    @Test
    void validateInitialOffer_fails_whenContractPolicyEvaluationFails() {

        var validatableOffer = createValidatableConsumerOffer();
        var participantAgent = new ParticipantAgent(emptyMap(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));

        when(policyEngine.evaluate(any(), isA(CatalogPolicyContext.class))).thenReturn(Result.success());
        when(policyEngine.evaluate(any(), isA(ContractNegotiationPolicyContext.class))).thenReturn(Result.failure("evaluation failure"));
        when(assetIndex.findById(anyString())).thenReturn(Asset.Builder.newInstance().build());
        when(assetIndex.countAssets(anyList())).thenReturn(1L);

        var result = validationService.validateInitialOffer(participantAgent, validatableOffer);

        assertThat(result).isFailed().detail()
                .contains("evaluation failure");
    }

    @ParameterizedTest
    @ValueSource(strings = { PROVIDER_ID })
    @NullSource
    void validateConsumerRequest_failsInvalidCredentials(String counterPartyId) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id("1")
                .counterPartyId(CONSUMER_ID)
                .counterPartyAddress("https://consumer.com")
                .protocol("test")
                .build();

        var participantAgent = new ParticipantAgent(Map.of(), counterPartyId != null ? Map.of(PARTICIPANT_IDENTITY, counterPartyId) : Map.of());

        var result = validationService.validateRequest(participantAgent, negotiation);

        assertThat(result.succeeded()).isFalse();

    }

    @Test
    void validateAgreement_failWhenOutsideInForcePeriod_fixed() {
        var participantAgent = new ParticipantAgent(emptyMap(), Map.of(PARTICIPANT_IDENTITY, CONSUMER_ID));
        when(policyEngine.evaluate(any(), isA(PolicyContext.class))).thenReturn(Result.failure("test-failure"));

        var agreement = createContractAgreement()
                .id(ContractOfferId.create("1", "2").toString())
                .contractSigningDate(Instant.now().toEpochMilli())
                .build();

        assertThat(validationService.validateAgreement(participantAgent, agreement)).isFailed()
                .detail().startsWith("Policy does not fulfill the agreement " + agreement.getId()).contains("test-failure");
    }

    private Result<ContractAgreement> validateAgreementDate(long signingDate) {
        when(policyEngine.evaluate(isA(Policy.class), isA(ContractNegotiationPolicyContext.class))).thenReturn(Result.success());

        var agreement = createContractAgreement()
                .id(ContractOfferId.create("1", "2").toString())
                .contractSigningDate(signingDate)
                .build();

        return validationService.validateAgreement(new ParticipantAgent(emptyMap(), emptyMap()), agreement);
    }

    private ContractOffer createContractOffer(Asset asset, Policy policy) {
        return ContractOffer.Builder.newInstance()
                .id(ContractOfferId.create("1", asset.getId()).toString())
                .assetId(asset.getId())
                .policy(policy)
                .build();
    }

    private ValidatableConsumerOffer createValidatableConsumerOffer(Asset asset, Policy policy) {
        return createValidatableConsumerOffer(asset, policy, policy);
    }

    private ValidatableConsumerOffer createValidatableConsumerOffer() {
        return createValidatableConsumerOffer(Asset.Builder.newInstance().build(), createPolicy());
    }

    private ValidatableConsumerOffer createValidatableConsumerOffer(Asset asset, Policy accessPolicy, Policy contractPolicy) {
        var offerId = ContractOfferId.create("1", asset.getId());
        return ValidatableConsumerOffer.Builder.newInstance()
                .offerId(offerId)
                .contractDefinition(createContractDefinition())
                .accessPolicy(accessPolicy)
                .contractPolicy(contractPolicy)
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

    private Policy createPolicy() {
        return Policy.Builder.newInstance().build();
    }

    private ContractAgreement.Builder createContractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .providerId(PROVIDER_ID)
                .consumerId(CONSUMER_ID)
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString());
    }
}

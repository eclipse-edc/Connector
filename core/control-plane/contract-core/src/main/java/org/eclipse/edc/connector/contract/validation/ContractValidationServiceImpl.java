/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - improvements, add policy engine
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.contract.validation;

import org.eclipse.edc.connector.contract.policy.PolicyEquality;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionService;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

import static java.lang.String.format;

/**
 * Implementation of the {@link ContractValidationService}.
 */
public class ContractValidationServiceImpl implements ContractValidationService {

    private final ParticipantAgentService agentService;
    private final ContractDefinitionService contractDefinitionService;
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyStore;
    private final PolicyEngine policyEngine;
    private final PolicyEquality policyEquality;
    private final Clock clock;

    public ContractValidationServiceImpl(ParticipantAgentService agentService,
                                         ContractDefinitionService contractDefinitionService,
                                         AssetIndex assetIndex,
                                         PolicyDefinitionStore policyStore,
                                         PolicyEngine policyEngine,
                                         PolicyEquality policyEquality,
                                         Clock clock) {
        this.agentService = agentService;
        this.contractDefinitionService = contractDefinitionService;
        this.assetIndex = assetIndex;
        this.policyStore = policyStore;
        this.policyEngine = policyEngine;
        this.policyEquality = policyEquality;
        this.clock = clock;
    }

    @Override
    @NotNull
    public Result<ContractOffer> validateInitialOffer(ClaimToken token, ContractOffer offer) {
        if (isMandatoryAttributeMissing(offer)) {
            return Result.failure("Mandatory attributes are missing.");
        }

        var contractId = ContractId.parse(offer.getId());
        if (!contractId.isValid()) {
            return Result.failure("Invalid id: " + offer.getId());
        }

        var agent = agentService.createFor(token);

        var consumerIdentity = agent.getIdentity();
        if (consumerIdentity == null) {
            return Result.failure("Invalid consumer identity");
        }

        var contractDefinition = contractDefinitionService.definitionFor(agent, contractId.definitionPart());
        if (contractDefinition == null) {
            return Result.failure(
                    "The ContractDefinition with id %s either does not exist or the access to it is not granted.");
        }

        var targetAsset = assetIndex.findById(offer.getAsset().getId());
        if (targetAsset == null) {
            return Result.failure("Invalid target: " + offer.getAsset().getId());
        }

        var contractPolicyDef = policyStore.findById(contractDefinition.getContractPolicyId());
        if (contractPolicyDef == null) {
            return Result.failure(format("Policy %s not found", contractDefinition.getContractPolicyId()));
        }

        var offerValidity = ChronoUnit.SECONDS.between(offer.getContractStart(), offer.getContractEnd());
        if (offerValidity != contractDefinition.getValidity()) {
            return Result.failure(format("Offer validity %ss does not match contract definition validity %ss", offerValidity, contractDefinition.getValidity()));
        }

        if (!policyEquality.test(contractPolicyDef.getPolicy().withTarget(targetAsset.getId()), offer.getPolicy())) {
            return Result.failure("Policy in the contract offer is not equal to the one in the contract definition");
        }

        var contractPolicyResult = policyEngine.evaluate(NEGOTIATION_SCOPE, contractPolicyDef.getPolicy(), agent);
        if (contractPolicyResult.failed()) {
            return Result.failure(format("Policy %s not fulfilled", contractPolicyDef.getUid()));
        }

        var validatedOffer = ContractOffer.Builder.newInstance()
                .id(offer.getId())
                .asset(targetAsset)
                .consumer(URI.create(consumerIdentity))   //m this must be set to the consumer identity
                .provider(offer.getProvider())
                .policy(contractPolicyDef.getPolicy())
                .contractStart(offer.getContractStart())
                .contractEnd(offer.getContractStart().plusSeconds(contractDefinition.getValidity()))
                .build();

        return Result.success(validatedOffer);
    }

    @Override
    @NotNull
    public Result<ContractAgreement> validateAgreement(ClaimToken token, ContractAgreement agreement) {
        var contractId = ContractId.parse(agreement.getId());
        if (!contractId.isValid()) {
            return Result.failure(format("The contract id %s does not follow the expected scheme", agreement.getId()));
        }

        if (!isStarted(agreement) || isExpired(agreement)) {
            return Result.failure("The agreement has not started yet or it has expired");
        }

        var agent = agentService.createFor(token);
        var consumerIdentity = agent.getIdentity();
        if (consumerIdentity == null || !consumerIdentity.equals(agreement.getConsumerAgentId())) {
            return Result.failure("Invalid provider credentials");
        }

        var policyResult = policyEngine.evaluate(NEGOTIATION_SCOPE, agreement.getPolicy(), agent);
        if (!policyResult.succeeded()) {
            return Result.failure(format("Policy does not fulfill the agreement %s, policy evaluation %s", agreement.getId(), policyResult.getFailureDetail()));
        }
        return Result.success(agreement);
    }

    @Override
    @NotNull
    public Result<Void> validateRequest(ClaimToken token, ContractNegotiation negotiation) {
        var agent = agentService.createFor(token);
        var counterPartyIdentity = agent.getIdentity();
        return counterPartyIdentity != null && counterPartyIdentity.equals(negotiation.getCounterPartyId()) ? Result.success() : Result.failure("Invalid counter-party identity");
    }

    @Override
    @NotNull
    public Result<Void> validateConfirmed(ClaimToken token, ContractAgreement agreement, ContractOffer latestOffer) {
        var contractId = ContractId.parse(agreement.getId());
        if (!contractId.isValid()) {
            return Result.failure(format("ContractId %s does not follow the expected schema.", agreement.getId()));
        }

        var agent = agentService.createFor(token);
        var providerIdentity = agent.getIdentity();
        if (providerIdentity == null || !providerIdentity.equals(agreement.getProviderAgentId())) {
            return Result.failure("Invalid provider credentials");
        }

        if (!policyEquality.test(agreement.getPolicy().withTarget(latestOffer.getAsset().getId()), latestOffer.getPolicy())) {
            return Result.failure("Policy in the contract agreement is not equal to the one in the contract offer");
        }

        return Result.success();
    }

    private boolean isExpired(ContractAgreement contractAgreement) {
        return contractAgreement.getContractEndDate() * 1000L < clock.millis();
    }

    private boolean isStarted(ContractAgreement contractAgreement) {
        return contractAgreement.getContractStartDate() * 1000L <= clock.millis();
    }

    private boolean isMandatoryAttributeMissing(ContractOffer offer) {
        // TODO add contractStart and contractEnd check as soon as the Infomodel serializer is used
        return offer.getProvider() == null || offer.getConsumer() == null;
    }
}

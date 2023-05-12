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
import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.contract.spi.validation.ValidatedConsumerOffer;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.String.format;
import static org.eclipse.edc.connector.contract.spi.ContractId.createContractId;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implementation of the {@link ContractValidationService}.
 */
public class ContractValidationServiceImpl implements ContractValidationService {

    private final String participantId;
    private final ParticipantAgentService agentService;
    private final ContractDefinitionResolver contractDefinitionResolver;
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyStore;
    private final PolicyEngine policyEngine;
    private final PolicyEquality policyEquality;

    public ContractValidationServiceImpl(String participantId,
                                         ParticipantAgentService agentService,
                                         ContractDefinitionResolver contractDefinitionResolver,
                                         AssetIndex assetIndex,
                                         PolicyDefinitionStore policyStore,
                                         PolicyEngine policyEngine,
                                         PolicyEquality policyEquality) {
        this.participantId = participantId;
        this.agentService = agentService;
        this.contractDefinitionResolver = contractDefinitionResolver;
        this.assetIndex = assetIndex;
        this.policyStore = policyStore;
        this.policyEngine = policyEngine;
        this.policyEquality = policyEquality;
    }

    @Override
    @NotNull
    public Result<ValidatedConsumerOffer> validateInitialOffer(ClaimToken token, ContractOffer offer) {
        var contractId = ContractId.parse(offer.getId());
        if (!contractId.isValid()) {
            return failure("Invalid id: " + offer.getId());
        }

        var agent = agentService.createFor(token);

        var result = validateInitialOffer(contractId, agent);

        return result.map(r ->
                new ValidatedConsumerOffer(agent.getIdentity(),
                        ContractOffer.Builder.newInstance()
                                .id(offer.getId())
                                .assetId(contractId.assetIdPart())
                                .providerId(participantId)
                                .policy(r.getPolicy())
                                .build())
        );
    }

    @Override
    public @NotNull Result<ValidatedConsumerOffer> validateInitialOffer(ClaimToken token, String offerId) {
        var contractId = ContractId.parse(offerId);
        if (!contractId.isValid()) {
            return failure("Invalid id: " + offerId);
        }

        var agent = agentService.createFor(token);

        var result = validateInitialOffer(contractId, agent);

        return result.map(r -> {
            var offer = createContractOffer(result.getContent().getDefinition(), result.getContent().getPolicy(), contractId.assetIdPart());
            return new ValidatedConsumerOffer(agent.getIdentity(), offer);
        });

    }

    @Override
    @NotNull
    public Result<ContractAgreement> validateAgreement(ClaimToken token, ContractAgreement agreement) {
        var contractId = ContractId.parse(agreement.getId());
        if (!contractId.isValid()) {
            return failure(format("The contract id %s does not follow the expected scheme", agreement.getId()));
        }

        var agent = agentService.createFor(token);
        var consumerIdentity = agent.getIdentity();
        if (consumerIdentity == null || !consumerIdentity.equals(agreement.getConsumerId())) {
            return failure("Invalid provider credentials");
        }

        // Create additional context information for policy engine to make agreement available in context
        var contextInformation = new HashMap<Class<?>, Object>();
        contextInformation.put(ContractAgreement.class, agreement);
        contextInformation.put(Instant.class, Instant.now());

        var policyResult = policyEngine.evaluate(TRANSFER_SCOPE, agreement.getPolicy(), agent, contextInformation);
        if (!policyResult.succeeded()) {
            return failure(format("Policy does not fulfill the agreement %s, policy evaluation %s", agreement.getId(), policyResult.getFailureDetail()));
        }
        return success(agreement);
    }

    @Override
    @NotNull
    public Result<Void> validateRequest(ClaimToken token, ContractNegotiation negotiation) {
        var agent = agentService.createFor(token);
        var counterPartyIdentity = agent.getIdentity();
        return counterPartyIdentity != null && counterPartyIdentity.equals(negotiation.getCounterPartyId()) ? success() : failure("Invalid counter-party identity");
    }

    @Override
    @NotNull
    public Result<Void> validateConfirmed(ClaimToken token, ContractAgreement agreement, ContractOffer latestOffer) {
        if (latestOffer == null) {
            return failure("No offer found");
        }

        var contractId = ContractId.parse(agreement.getId());
        if (!contractId.isValid()) {
            return failure(format("ContractId %s does not follow the expected schema.", agreement.getId()));
        }

        var agent = agentService.createFor(token);
        var providerIdentity = agent.getIdentity();
        if (providerIdentity == null || !providerIdentity.equals(agreement.getProviderId())) {
            return failure("Invalid provider credentials");
        }

        if (!policyEquality.test(agreement.getPolicy().withTarget(latestOffer.getAssetId()), latestOffer.getPolicy())) {
            return failure("Policy in the contract agreement is not equal to the one in the contract offer");
        }

        return success();
    }

    /**
     * Validates an initial contract offer, ensuring that the referenced asset exists, is selected by the corresponding policy definition and the agent fulfills the contract policy.
     * A sanitized policy definition is returned to avoid clients injecting manipulated policies.
     */
    private Result<SanitizedResult> validateInitialOffer(ContractId contractId, ParticipantAgent agent) {
        var consumerIdentity = agent.getIdentity();
        if (consumerIdentity == null) {
            return failure("Invalid consumer identity");
        }

        var contractDefinition = contractDefinitionResolver.definitionFor(agent, contractId.definitionPart());
        if (contractDefinition == null) {
            return failure("The ContractDefinition with id %s either does not exist or the access to it is not granted.");
        }

        // verify the target asset exists
        var targetAsset = assetIndex.findById(contractId.assetIdPart());
        if (targetAsset == null) {
            return failure("Invalid target: " + contractId.assetIdPart());
        }

        // verify that the asset in the offer is actually in the contract definition
        var testCriteria = new ArrayList<>(contractDefinition.getSelectorExpression().getCriteria());
        testCriteria.add(new Criterion(Asset.PROPERTY_ID, "=", contractId.assetIdPart()));
        if (assetIndex.countAssets(testCriteria) <= 0) {
            return failure("Asset ID from the ContractOffer is not included in the ContractDefinition");
        }

        var policyDefinition = policyStore.findById(contractDefinition.getContractPolicyId());
        if (policyDefinition == null) {
            return failure(format("Policy %s not found", contractDefinition.getContractPolicyId()));
        }

        var policy = policyDefinition.getPolicy().withTarget(contractId.assetIdPart());

        var policyResult = policyEngine.evaluate(NEGOTIATION_SCOPE, policy, agent);
        if (policyResult.failed()) {
            return failure(format("Policy %s not fulfilled", policyDefinition.getUid()));
        }
        return Result.success(new SanitizedResult(contractDefinition, policy));
    }

    @NotNull
    private ContractOffer createContractOffer(ContractDefinition definition, Policy policy, String assetId) {
        return ContractOffer.Builder.newInstance()
                .id(createContractId(definition.getId(), assetId))
                .providerId(participantId)
                .policy(policy)
                .assetId(assetId)
                .build();
    }

    private static class SanitizedResult {
        private final ContractDefinition definition;
        private final Policy policy;

        SanitizedResult(ContractDefinition definition, Policy policy) {
            this.definition = definition;
            this.policy = policy;
        }

        ContractDefinition getDefinition() {
            return definition;
        }

        Policy getPolicy() {
            return policy;
        }
    }


}

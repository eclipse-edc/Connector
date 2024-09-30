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

package org.eclipse.edc.connector.controlplane.contract.validation;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.policy.PolicyEquality;
import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ValidatableConsumerOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ValidatedConsumerOffer;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.contract.ContractCoreExtension.CATALOG_SCOPE;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implementation of the {@link ContractValidationService}.
 */
public class ContractValidationServiceImpl implements ContractValidationService {

    private final AssetIndex assetIndex;
    private final PolicyEngine policyEngine;
    private final PolicyEquality policyEquality;

    public ContractValidationServiceImpl(AssetIndex assetIndex,
                                         PolicyEngine policyEngine,
                                         PolicyEquality policyEquality) {
        this.assetIndex = assetIndex;
        this.policyEngine = policyEngine;
        this.policyEquality = policyEquality;
    }

    @Override
    public @NotNull Result<ValidatedConsumerOffer> validateInitialOffer(ParticipantAgent agent, ValidatableConsumerOffer consumerOffer) {
        return validateInitialOffer(consumerOffer, agent)
                .compose(policy -> createContractOffer(policy, consumerOffer.getOfferId()))
                .map(contractOffer -> new ValidatedConsumerOffer(agent.getIdentity(), contractOffer));
    }

    @Override
    public @NotNull Result<ContractAgreement> validateAgreement(ParticipantAgent agent, ContractAgreement agreement) {
        var consumerIdentity = agent.getIdentity();
        if (consumerIdentity == null || !consumerIdentity.equals(agreement.getConsumerId())) {
            return failure("Invalid provider credentials");
        }

        var policyContext = PolicyContextImpl.Builder.newInstance()
                .additional(ParticipantAgent.class, agent)
                .additional(ContractAgreement.class, agreement)
                .additional(Instant.class, Instant.now())
                .build();
        var policyResult = policyEngine.evaluate(TRANSFER_SCOPE, agreement.getPolicy(), policyContext);
        if (!policyResult.succeeded()) {
            return failure(format("Policy does not fulfill the agreement %s, policy evaluation %s", agreement.getId(), policyResult.getFailureDetail()));
        }
        return success(agreement);
    }

    @Override
    public @NotNull Result<Void> validateRequest(ParticipantAgent agent, ContractAgreement agreement) {
        return Optional.ofNullable(agent.getIdentity())
                .filter(id -> id.equals(agreement.getConsumerId()) || id.equals(agreement.getProviderId()))
                .map(id -> Result.success())
                .orElse(Result.failure("Invalid counter-party identity"));
    }

    @Override
    public @NotNull Result<Void> validateRequest(ParticipantAgent agent, ContractNegotiation negotiation) {
        var counterPartyIdentity = agent.getIdentity();
        return counterPartyIdentity != null && counterPartyIdentity.equals(negotiation.getCounterPartyId()) ? success() : failure("Invalid counter-party identity");
    }

    @Override
    public @NotNull Result<Void> validateConfirmed(ParticipantAgent agent, ContractAgreement agreement, ContractOffer latestOffer) {
        if (!Objects.equals(agent.getIdentity(), agreement.getProviderId())) {
            return failure("Invalid provider credentials");
        }

        if (latestOffer == null) {
            return failure("No offer found");
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
    private Result<Policy> validateInitialOffer(ValidatableConsumerOffer consumerOffer, ParticipantAgent agent) {
        var consumerIdentity = agent.getIdentity();
        if (consumerIdentity == null) {
            return failure("Invalid consumer identity");
        }

        var accessPolicyResult = evaluatePolicy(consumerOffer.getAccessPolicy(), CATALOG_SCOPE, agent, consumerOffer.getOfferId());

        if (accessPolicyResult.failed()) {
            return accessPolicyResult;
        }

        // verify the target asset exists
        var targetAsset = assetIndex.findById(consumerOffer.getOfferId().assetIdPart());
        if (targetAsset == null) {
            return failure("Invalid target: " + consumerOffer.getOfferId().assetIdPart());
        }

        // verify that the asset in the offer is actually in the contract definition
        var testCriteria = new ArrayList<>(consumerOffer.getContractDefinition().getAssetsSelector());
        testCriteria.add(new Criterion(Asset.PROPERTY_ID, "=", consumerOffer.getOfferId().assetIdPart()));
        if (assetIndex.countAssets(testCriteria) <= 0) {
            return failure("Asset ID from the ContractOffer is not included in the ContractDefinition");
        }

        var contractPolicy = consumerOffer.getContractPolicy().withTarget(consumerOffer.getOfferId().assetIdPart());
        return evaluatePolicy(contractPolicy, NEGOTIATION_SCOPE, agent, consumerOffer.getOfferId());
    }

    private Result<Policy> evaluatePolicy(Policy policy, String scope, ParticipantAgent agent, ContractOfferId offerId) {
        var policyContext = PolicyContextImpl.Builder.newInstance().additional(ParticipantAgent.class, agent).build();
        var policyResult = policyEngine.evaluate(scope, policy, policyContext);
        if (policyResult.failed()) {
            return failure(format("Policy in scope %s not fulfilled for offer %s, policy evaluation %s", scope, offerId.toString(), policyResult.getFailureDetail()));
        }
        return Result.success(policy);
    }

    @NotNull
    private Result<ContractOffer> createContractOffer(Policy policy, ContractOfferId contractOfferId) {
        if (!contractOfferId.assetIdPart().equals(policy.getTarget())) {
            return Result.failure("Policy target %s does not match the asset ID in the contract offer %s".formatted(policy.getTarget(), contractOfferId.assetIdPart()));
        }
        return Result.success(ContractOffer.Builder.newInstance()
                .id(contractOfferId.toString())
                .policy(policy)
                .assetId(contractOfferId.assetIdPart())
                .build());
    }

}

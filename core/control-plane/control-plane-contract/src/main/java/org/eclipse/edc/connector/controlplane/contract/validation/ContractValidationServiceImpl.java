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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - improvements, add policy engine
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.contract.validation;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.policy.PolicyEquality;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ValidatableConsumerOffer;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

import static java.lang.String.format;
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
    public @NotNull Result<Void> validateInitialOffer(ParticipantAgent agent, ValidatableConsumerOffer consumerOffer) {
        var accessPolicyResult = policyEngine.evaluate(consumerOffer.getAccessPolicy(), new CatalogPolicyContext(agent));
        if (accessPolicyResult.failed()) {
            return accessPolicyResult.mapFailure();
        }

        var target = consumerOffer.getOfferId().assetIdPart();
        if (assetIndex.findById(target) == null) {
            return failure("Invalid target: " + target);
        }

        // verify that the asset in the offer is actually in the contract definition
        var testCriteria = new ArrayList<>(consumerOffer.getContractDefinition().getAssetsSelector());
        testCriteria.add(new Criterion(Asset.PROPERTY_ID, "=", target));
        if (assetIndex.countAssets(testCriteria) <= 0) {
            return failure("Asset ID from the ContractOffer is not included in the ContractDefinition");
        }

        var contractOfferId = consumerOffer.getOfferId();
        if (!contractOfferId.assetIdPart().equals(target)) {
            return failure("Policy target %s does not match the asset ID in the contract offer %s".formatted(target, contractOfferId.assetIdPart()));
        }

        var contractPolicy = consumerOffer.getTargetedContractPolicy();
        var contractPolicyResult = policyEngine.evaluate(contractPolicy, new ContractNegotiationPolicyContext(agent));
        if (contractPolicyResult.failed()) {
            return contractPolicyResult.mapFailure();
        }

        return Result.success();
    }

    @Override
    public @NotNull Result<ContractAgreement> validateAgreement(ParticipantAgent agent, ContractAgreement agreement) {
        if (!Objects.equals(agent.getIdentity(), agreement.getConsumerId())) {
            return failure("Invalid provider credentials");
        }

        var policyContext = new TransferProcessPolicyContext(agent, agreement, Instant.now());
        var policyResult = policyEngine.evaluate(agreement.getPolicy(), policyContext);
        if (!policyResult.succeeded()) {
            return failure(format("Policy does not fulfill the agreement %s, policy evaluation %s", agreement.getId(), policyResult.getFailureDetail()));
        }
        return success(agreement);
    }

    @Override
    public @NotNull Result<Void> validateRequest(ParticipantAgent agent, ContractAgreement agreement) {
        if (agent.getIdentity().equals(agreement.getConsumerId()) || agent.getIdentity().equals(agreement.getProviderId())) {
            return Result.success();
        }

        return Result.failure("Invalid counter-party identity");
    }

    @Override
    public @NotNull Result<Void> validateRequest(ParticipantAgent agent, ContractNegotiation negotiation) {
        if (agent.getIdentity().equals(negotiation.getCounterPartyId())) {
            return success();
        }

        return failure("Invalid counter-party identity");
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
        var accessPolicyResult = policyEngine.evaluate(consumerOffer.getAccessPolicy(), new CatalogPolicyContext(agent));

        if (accessPolicyResult.failed()) {
            return accessPolicyResult.mapFailure();
        }

        // verify the target asset exists
        var target = consumerOffer.getOfferId().assetIdPart();
        var targetAsset = assetIndex.findById(target);
        if (targetAsset == null) {
            return failure("Invalid target: " + target);
        }

        // verify that the asset in the offer is actually in the contract definition
        var testCriteria = new ArrayList<>(consumerOffer.getContractDefinition().getAssetsSelector());
        testCriteria.add(new Criterion(Asset.PROPERTY_ID, "=", target));
        if (assetIndex.countAssets(testCriteria) <= 0) {
            return failure("Asset ID from the ContractOffer is not included in the ContractDefinition");
        }

        var contractOfferId = consumerOffer.getOfferId();
        if (!contractOfferId.assetIdPart().equals(target)) {
            return failure("Policy target %s does not match the asset ID in the contract offer %s".formatted(target, contractOfferId.assetIdPart()));
        }

        var contractPolicy = consumerOffer.getContractPolicy().withTarget(target);
        return policyEngine.evaluate(contractPolicy, new ContractNegotiationPolicyContext(agent))
                .map(v -> contractPolicy);
    }

}

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
 *       Fraunhofer Institute for Software and Systems Engineering - improvements
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.contract.validation;

import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.ArrayList;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.contract.common.ContractId.DEFINITION_PART;
import static org.eclipse.dataspaceconnector.contract.common.ContractId.parseContractId;

/**
 * Implementation of the {@link ContractValidationService}.
 */
public class ContractValidationServiceImpl implements ContractValidationService {

    private final ParticipantAgentService agentService;
    private final ContractDefinitionService contractDefinitionService;
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyStore;
    private final Clock clock;

    public ContractValidationServiceImpl(ParticipantAgentService agentService, ContractDefinitionService contractDefinitionService, AssetIndex assetIndex, PolicyDefinitionStore policyStore, Clock clock) {
        this.agentService = agentService;
        this.contractDefinitionService = contractDefinitionService;
        this.assetIndex = assetIndex;
        this.policyStore = policyStore;
        this.clock = clock;
    }

    @Override
    @NotNull
    public Result<ContractOffer> validate(ClaimToken token, ContractOffer offer) {
        if (isMandatoryAttributeMissing(offer)) {
            return Result.failure("Mandatory attributes are missing.");
        }

        var contractIdTokens = parseContractId(offer.getId());
        if (contractIdTokens.length != 2) {
            return Result.failure("Invalid id: " + offer.getId());
        }

        var agent = agentService.createFor(token);
        var contractDefinition = contractDefinitionService.definitionFor(agent, contractIdTokens[DEFINITION_PART]);
        if (contractDefinition == null) {
            return Result.failure("Invalid contract.");
        }

        // take asset from definition and index
        var criteria = createCriteria(offer, contractDefinition);
        var targetAssets = assetIndex.queryAssets(QuerySpec.Builder.newInstance().filter(criteria).build());
        var targetAsset = targetAssets.findFirst().orElse(null);
        if (targetAsset == null) {
            return Result.failure("Invalid target: " + offer.getAsset().getId());
        }

        var contractPolicyDef = policyStore.findById(contractDefinition.getContractPolicyId());
        if (contractPolicyDef == null) {
            return Result.failure(format("Policy %s not found", contractDefinition.getContractPolicyId()));
        }

        var validatedOffer = ContractOffer.Builder.newInstance()
                .id(offer.getId())
                .asset(targetAsset)
                .policy(contractPolicyDef.getPolicy())
                .build();

        return Result.success(validatedOffer);
    }

    @Override
    public @NotNull Result<ContractOffer> validate(ClaimToken token, ContractOffer offer, ContractOffer latestOffer) {
        if (isMandatoryAttributeMissing(offer)) {
            return Result.failure("Mandatory attributes are missing.");
        }

        var contractIdTokens = parseContractId(offer.getId());
        if (contractIdTokens.length != 2) {
            return Result.failure("Invalid id: " + offer.getId());
        }

        // TODO implement validation against latest offer within the negotiation

        return Result.success(null);
    }

    @Override
    public boolean validate(ClaimToken token, ContractAgreement agreement) {
        var agent = agentService.createFor(token);
        var tokens = parseContractId(agreement.getId());
        if (tokens.length != 2) {
            return false; // not a valid id
        }

        if (!isStarted(agreement) || isExpired(agreement)) {
            return false;
        }

        return contractDefinitionService.definitionFor(agent, tokens[DEFINITION_PART]) != null;
        // TODO validate counter-party
    }

    @Override
    public boolean validate(ClaimToken token, ContractAgreement agreement, ContractOffer latestOffer) {
        // TODO implement validation against latest offer within the negotiation
        var contractIdTokens = parseContractId(agreement.getId());
        return contractIdTokens.length == 2;
    }

    @NotNull
    private ArrayList<Criterion> createCriteria(ContractOffer offer, ContractDefinition contractDefinition) {
        var criteria = new ArrayList<>(contractDefinition.getSelectorExpression().getCriteria());
        var criterion = new Criterion(Asset.PROPERTY_ID, "=", offer.getAsset().getId());
        criteria.add(criterion);
        return criteria;
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

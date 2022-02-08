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
 *
 */
package org.eclipse.dataspaceconnector.contract.validation;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

import static org.eclipse.dataspaceconnector.contract.common.ContractId.DEFINITION_PART;
import static org.eclipse.dataspaceconnector.contract.common.ContractId.parseContractId;

/**
 * Implementation of the {@link ContractValidationService}.
 */
public class ContractValidationServiceImpl implements ContractValidationService {

    private final ParticipantAgentService agentService;
    private final Supplier<ContractDefinitionService> definitionServiceSupplier;
    private final AssetIndex assetIndex;

    public ContractValidationServiceImpl(ParticipantAgentService agentService, Supplier<ContractDefinitionService> definitionServiceSupplier, AssetIndex assetIndex) {
        this.agentService = Objects.requireNonNull(agentService);
        this.definitionServiceSupplier = Objects.requireNonNull(definitionServiceSupplier);
        this.assetIndex = Objects.requireNonNull(assetIndex);
    }

    @Override
    @NotNull
    public Result<ContractOffer> validate(ClaimToken token, ContractOffer offer) {
        var agent = agentService.createFor(token);
        var contractIdTokens = parseContractId(offer.getId());
        if (contractIdTokens.length != 2) {
            return Result.failure("Not a valid id: " + offer.getId());
        }

        // validate the offer against the original definition, which includes policy verification
        var contractDefinition = definitionServiceSupplier.get().definitionFor(agent, contractIdTokens[DEFINITION_PART]);
        if (contractDefinition == null) {
            return Result.failure("");
        }

        // take asset from definition and index
        var criteria = createCriteria(offer, contractDefinition);
        var targetAssets = assetIndex.queryAssets(criteria);
        var targetAsset = targetAssets.findFirst().orElse(null);
        if (targetAsset == null) {
            return Result.failure("");
        }

        // TODO Hand over to external PDP
        // TODO create counter offer if wanted

        var sanitizedUsagePolicy = contractDefinition.getContractPolicy();
        var validatedOffer = ContractOffer.Builder.newInstance()
                .id(offer.getId())
                .asset(targetAsset)
                .policy(sanitizedUsagePolicy)
                .build();
        return Result.success(validatedOffer);
    }

    @Override
    public @NotNull Result<ContractOffer> validate(ClaimToken token, ContractOffer offer, ContractOffer latestOffer) {
        var agent = agentService.createFor(token);
        var contractIdTokens = parseContractId(offer.getId());
        if (contractIdTokens.length != 2) {
            return Result.failure("Not a valid id: " + offer.getId());
        }

        // TODO implement validation
        // TODO Hand over to external PDP
        // TODO create counter offer if wanted

        return Result.success(null);
    }

    @Override
    public boolean validate(ClaimToken token, ContractAgreement agreement) {
        var agent = agentService.createFor(token);
        var tokens = parseContractId(agreement.getId());
        if (tokens.length != 2) {
            // not a valid id
            return false;
        }

        if (!isStarted(agreement)) {
            return false;
        }

        if (isExpired(agreement)) {
            return false;
        }

        return definitionServiceSupplier.get().definitionFor(agent, tokens[DEFINITION_PART]) != null;
        // TODO validate counter-party
    }

    @Override
    public boolean validate(ClaimToken token, ContractAgreement agreement, ContractOffer latestOffer) {
        // TODO implement validation
        // TODO Hand over to external PDP
        // TODO create counter offer if wanted

        return true;
    }

    @NotNull
    private ArrayList<Criterion> createCriteria(ContractOffer offer, ContractDefinition contractDefinition) {
        var criteria = new ArrayList<>(contractDefinition.getSelectorExpression().getCriteria());
        var criterion = new Criterion(Asset.PROPERTY_ID, "=", offer.getAsset().getId());
        criteria.add(criterion);
        return criteria;
    }

    private boolean isExpired(ContractAgreement contractAgreement) {
        return contractAgreement.getContractEndDate() < Instant.now().getEpochSecond();
    }

    private boolean isStarted(ContractAgreement contractAgreement) {
        return contractAgreement.getContractStartDate() <= Instant.now().getEpochSecond();
    }
}

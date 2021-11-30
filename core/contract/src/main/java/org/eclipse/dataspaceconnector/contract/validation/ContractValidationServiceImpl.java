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
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.contract.validation.OfferValidationResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    public OfferValidationResult validate(ClaimToken token, ContractOffer offer) {
        var agent = agentService.createFor(token);
        var contractIdTokens = parseContractId(offer.getId());
        if (contractIdTokens.length != 2) {
            // not a valid id
            return OfferValidationResult.INVALID;
        }

        // validate the offer against the original definition, which includes policy verification
        var contractDefinition = definitionServiceSupplier.get().definitionFor(agent, contractIdTokens[DEFINITION_PART]);
        if (contractDefinition == null) {
            return OfferValidationResult.INVALID;
        }

        // take asset from definition and index
        var criteria = createCriteria(offer, contractDefinition);
        var targetAssets = assetIndex.queryAssets(criteria);
        var targetAsset = targetAssets.findFirst().orElse(null);
        if (targetAsset == null) {
            return OfferValidationResult.INVALID;
        }

        var sanitizedUsagePolicy = contractDefinition.getContractPolicy();
        var validatedOffer = ContractOffer.Builder.newInstance().id(offer.getId()).asset(targetAsset).policy(sanitizedUsagePolicy).build();
        return new OfferValidationResult(validatedOffer);
    }

    @Override
    public boolean validate(ClaimToken token, ContractAgreement agreement) {
        var agent = agentService.createFor(token);
        var tokens = parseContractId(agreement.getId());
        if (tokens.length != 2) {
            // not a valid id
            return false;
        }
        return definitionServiceSupplier.get().definitionFor(agent, tokens[DEFINITION_PART]) != null;
        // TODO validate counter-party
    }

    @NotNull
    private ArrayList<Criterion> createCriteria(ContractOffer offer, ContractDefinition contractDefinition) {
        var criteria = new ArrayList<>(contractDefinition.getSelectorExpression().getCriteria());
        var criterion = new Criterion(Asset.PROPERTY_ID, "=", offer.getAsset().getId());
        criteria.add(criterion);
        return criteria;
    }

}

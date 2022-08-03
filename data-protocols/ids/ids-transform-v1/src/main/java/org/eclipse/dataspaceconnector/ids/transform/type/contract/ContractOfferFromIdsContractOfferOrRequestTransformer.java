/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.dataspaceconnector.ids.transform.type.contract;

import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsType;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.transform.type.contract.PropertyUtil.addIdsContractPropertiesToPolicy;

/**
 * Transforms an IDS ContractRequest into an {@link ContractOffer}.
 */
public class ContractOfferFromIdsContractOfferOrRequestTransformer implements IdsTypeTransformer<ContractTransformerInput, ContractOffer> {

    @Override
    public Class<ContractTransformerInput> getInputType() {
        return ContractTransformerInput.class;
    }

    @Override
    public Class<ContractOffer> getOutputType() {
        return ContractOffer.class;
    }

    @Override
    public @Nullable ContractOffer transform(ContractTransformerInput object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var contract = object.getContract();
        var asset = object.getAsset();

        var edcPermissions = new ArrayList<Permission>();
        var edcProhibitions = new ArrayList<Prohibition>();
        var edcObligations = new ArrayList<Duty>();

        if (contract.getPermission() != null) {
            for (var idsPermission : contract.getPermission()) {
                edcPermissions.add(context.transform(idsPermission, Permission.class));
            }
        }

        if (contract.getProhibition() != null) {
            for (var idsProhibition : contract.getProhibition()) {
                edcProhibitions.add(context.transform(idsProhibition, Prohibition.class));
            }
        }

        if (contract.getObligation() != null) {
            for (var idsObligation : contract.getObligation()) {
                edcObligations.add(context.transform(idsObligation, Duty.class));
            }
        }

        var policyBuilder = Policy.Builder.newInstance();

        policyBuilder.duties(edcObligations);
        policyBuilder.prohibitions(edcProhibitions);
        policyBuilder.permissions(edcPermissions);

        var policy = addIdsContractPropertiesToPolicy(contract.getProperties(), policyBuilder).build();

        var contractOfferBuilder = ContractOffer.Builder.newInstance()
                .policy(policy)
                .consumer(contract.getConsumer())
                .provider(contract.getProvider())
                .asset(asset);

        var result = IdsId.from(contract.getId().toString());
        if (result.failed()) {
            context.reportProblem("id of incoming IDS contract offer/request expected to be not null");
            // contract offer builder requires id to be not null
            return null;
        }

        var idsId = result.getContent();
        if (!List.of(IdsType.CONTRACT_REQUEST, IdsType.CONTRACT_OFFER).contains(idsId.getType())) {
            context.reportProblem("handled object is not of type IDS contract offer/request");
        }

        contractOfferBuilder.id(idsId.getValue());

        if (contract.getContractEnd() != null) {
            contractOfferBuilder.contractEnd(contract.getContractEnd().toGregorianCalendar().toZonedDateTime());
        }

        if (contract.getContractStart() != null) {
            contractOfferBuilder.contractStart(contract.getContractStart().toGregorianCalendar().toZonedDateTime());
        }

        return contractOfferBuilder.build();
    }
}

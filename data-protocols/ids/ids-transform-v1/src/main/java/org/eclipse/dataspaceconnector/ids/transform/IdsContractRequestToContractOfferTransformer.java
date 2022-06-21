/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.ContractRequest;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Transforms an IDS ContractRequest into an {@link ContractOffer}.
 */
public class IdsContractRequestToContractOfferTransformer implements IdsTypeTransformer<ContractTransformerInput, ContractOffer> {

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

        var contractRequest = (ContractRequest) object.getContract();
        var asset = object.getAsset();

        var edcPermissions = new ArrayList<Permission>();
        var edcProhibitions = new ArrayList<Prohibition>();
        var edcObligations = new ArrayList<Duty>();

        if (contractRequest.getPermission() != null) {
            for (var edcPermission : contractRequest.getPermission()) {
                var idsPermission = context.transform(edcPermission, Permission.class);
                edcPermissions.add(idsPermission);
            }
        }

        if (contractRequest.getProhibition() != null) {
            for (var edcProhibition : contractRequest.getProhibition()) {
                var idsProhibition = context.transform(edcProhibition, Prohibition.class);
                edcProhibitions.add(idsProhibition);
            }
        }

        if (contractRequest.getObligation() != null) {
            for (var edcObligation : contractRequest.getObligation()) {
                var idsObligation = context.transform(edcObligation, Duty.class);
                edcObligations.add(idsObligation);
            }
        }

        var policyBuilder = Policy.Builder.newInstance();

        policyBuilder.duties(edcObligations);
        policyBuilder.prohibitions(edcProhibitions);
        policyBuilder.permissions(edcPermissions);

        var contractOfferBuilder = ContractOffer.Builder.newInstance()
                .policy(policyBuilder.build())
                .consumer(contractRequest.getConsumer())
                .provider(contractRequest.getProvider())
                .asset(asset);

        var idsUri = contractRequest.getId();
        if (idsUri != null) {
            var id = IdsIdParser.parse(idsUri.toString());
            try {
                if (id.getType() != IdsType.CONTRACT_REQUEST && id.getType() != IdsType.CONTRACT_OFFER) {
                    context.reportProblem("handled id is not of typ contract request or offer");
                }

                contractOfferBuilder.id(id.getValue());
            } catch (NullPointerException e) {
                context.reportProblem("cannot handle empty ids id");
            }
        }

        if (contractRequest.getContractEnd() != null) {
            contractOfferBuilder.contractEnd(contractRequest.getContractEnd().toGregorianCalendar().toZonedDateTime());
        }

        if (contractRequest.getContractStart() != null) {
            contractOfferBuilder.contractStart(contractRequest.getContractStart().toGregorianCalendar().toZonedDateTime());
        }

        return contractOfferBuilder.build();
    }
}

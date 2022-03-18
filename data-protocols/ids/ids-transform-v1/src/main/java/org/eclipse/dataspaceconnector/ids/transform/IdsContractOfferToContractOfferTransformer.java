/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Transforms an IDS ContractOffer into an {@link ContractOffer}.
 * Please note that, while the {@link ContractOffer} may contain multiple {@link Asset} this mapping is done in IDS
 * via the {@link de.fraunhofer.iais.eis.Resource} object. Therefore, this mapping can only create empty {@link ContractOffer}
 * without any {@link Asset}.
 */
public class IdsContractOfferToContractOfferTransformer implements IdsTypeTransformer<de.fraunhofer.iais.eis.ContractOffer, ContractOffer> {

    @Override
    public Class<de.fraunhofer.iais.eis.ContractOffer> getInputType() {
        return de.fraunhofer.iais.eis.ContractOffer.class;
    }

    @Override
    public Class<ContractOffer> getOutputType() {
        return ContractOffer.class;
    }

    @Override
    public @Nullable ContractOffer transform(de.fraunhofer.iais.eis.ContractOffer object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var edcPermissions = new ArrayList<Permission>();
        var edcProhibitions = new ArrayList<Prohibition>();
        var edcObligations = new ArrayList<Duty>();

        if (object.getPermission() != null) {
            for (var edcPermission : object.getPermission()) {
                var idsPermission = context.transform(edcPermission, Permission.class);
                edcPermissions.add(idsPermission);
            }
        }

        if (object.getProhibition() != null) {
            for (var edcProhibition : object.getProhibition()) {
                var idsProhibition = context.transform(edcProhibition, Prohibition.class);
                edcProhibitions.add(idsProhibition);
            }
        }

        if (object.getObligation() != null) {
            for (var edcObligation : object.getObligation()) {
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
                .consumer(object.getConsumer())
                .provider(object.getProvider());

        var idsUri = object.getId();
        if (idsUri != null) {
            var id = IdsIdParser.parse(idsUri.toString());
            try {
                if (id.getType() != IdsType.CONTRACT_OFFER) {
                    context.reportProblem("handled id is not of typ contract offer");
                }

                contractOfferBuilder.id(id.getValue());
            } catch (NullPointerException e) {
                context.reportProblem("cannot handle empty ids id");
            }
        }

        if (object.getContractEnd() != null) {
            contractOfferBuilder.contractEnd(
                    object.getContractEnd().toGregorianCalendar().toZonedDateTime());
        }

        if (object.getContractStart() != null) {
            contractOfferBuilder.contractStart(
                    object.getContractStart().toGregorianCalendar().toZonedDateTime());
        }

        return contractOfferBuilder.build();
    }
}

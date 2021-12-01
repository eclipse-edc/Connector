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
 *       Fraunhofer Institute for Software and Systems Engineering - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Transforms an IDS ContractAgreement into an {@link ContractAgreement}.
 */
public class IdsContractAgreementToContractAgreementTransformer implements IdsTypeTransformer<de.fraunhofer.iais.eis.ContractAgreement, ContractAgreement> {

    @Override
    public Class<de.fraunhofer.iais.eis.ContractAgreement> getInputType() {
        return de.fraunhofer.iais.eis.ContractAgreement.class;
    }

    @Override
    public Class<ContractAgreement> getOutputType() {
        return ContractAgreement.class;
    }

    @Override
    public @Nullable ContractAgreement transform(de.fraunhofer.iais.eis.ContractAgreement object, @NotNull TransformerContext context) {
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

        var builder = ContractAgreement.Builder.newInstance()
                .policy(policyBuilder.build())
                .consumerAgentId(object.getConsumer())
                .providerAgentId(object.getProvider());

        if (object.getId() != null) {
            builder.id(object.getId().toString());
        }

        if (object.getContractEnd() != null) {
            builder.contractEndDate(object.getContractEnd().toGregorianCalendar().toZonedDateTime());
        }

        if (object.getContractStart() != null) {
            builder.contractStartDate(object.getContractStart().toGregorianCalendar().toZonedDateTime());
        }

        if (object.getContractDate() != null) {
            builder.contractSigningDate(object.getContractDate().toGregorianCalendar().toZonedDateTime());
        }

        return builder.build();
    }
}

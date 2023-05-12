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
 *       Fraunhofer Institute for Software and Systems Engineering - Initial Implementation
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.contract;

import de.fraunhofer.iais.eis.Contract;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.ids.spi.transform.ContractAgreementTransformerOutput;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transforms an IDS ContractAgreement into an {@link ContractAgreement}.
 */
public class ContractAgreementFromIdsContractAgreementTransformer implements IdsTypeTransformer<de.fraunhofer.iais.eis.ContractAgreement, ContractAgreementTransformerOutput> {

    @Override
    public Class<de.fraunhofer.iais.eis.ContractAgreement> getInputType() {
        return de.fraunhofer.iais.eis.ContractAgreement.class;
    }

    @Override
    public Class<ContractAgreementTransformerOutput> getOutputType() {
        return ContractAgreementTransformerOutput.class;
    }

    @Override
    public @Nullable ContractAgreementTransformerOutput transform(de.fraunhofer.iais.eis.@NotNull ContractAgreement contractAgreement, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (contractAgreement == null) {
            return null;
        }

        if (contractAgreement.getPermission().isEmpty()) {
            context.reportProblem("ContractAgreement's Policy should contain at least one permission");
            return null;
        }

        var edcPermissions = Optional.of(contractAgreement)
                .map(Contract::getPermission)
                .stream().flatMap(Collection::stream)
                .map(it -> context.transform(it, Permission.class))
                .collect(Collectors.toList());

        var edcProhibitions = Optional.of(contractAgreement)
                .map(Contract::getProhibition)
                .stream().flatMap(Collection::stream)
                .map(it -> context.transform(it, Prohibition.class))
                .collect(Collectors.toList());

        var edcObligations = Optional.of(contractAgreement)
                .map(Contract::getObligation)
                .stream().flatMap(Collection::stream)
                .map(it -> context.transform(it, Duty.class))
                .collect(Collectors.toList());

        var policyBuilder = Policy.Builder.newInstance();

        policyBuilder.duties(edcObligations);
        policyBuilder.prohibitions(edcProhibitions);
        policyBuilder.permissions(edcPermissions);

        var policy = PropertyUtil.addIdsContractPropertiesToPolicy(contractAgreement.getProperties(), policyBuilder).build();

        var assetId = edcPermissions.get(0).getTarget();

        var builder = ContractAgreement.Builder.newInstance()
                .policy(policy)
                .consumerId(String.valueOf(contractAgreement.getConsumer()))
                .providerId(String.valueOf(contractAgreement.getProvider()))
                .assetId(assetId);

        var result = IdsId.from(contractAgreement.getId());
        if (result.failed()) {
            context.reportProblem("id of incoming IDS contract agreement expected to be not null");
            // contract agreement builder requires id to be not null
            return null;
        }

        var idsId = result.getContent();
        if (!idsId.getType().equals(IdsType.CONTRACT_AGREEMENT)) {
            context.reportProblem("handled object is not of type IDS contract agreement");
        }

        builder.id(idsId.getValue());


        if (contractAgreement.getContractDate() != null) {
            builder.contractSigningDate(contractAgreement.getContractDate().toGregorianCalendar().toZonedDateTime().toEpochSecond());
        }

        return ContractAgreementTransformerOutput.Builder.newInstance()
                .contractAgreement(builder.build())
                .policy(policy)
                .build();
    }
}

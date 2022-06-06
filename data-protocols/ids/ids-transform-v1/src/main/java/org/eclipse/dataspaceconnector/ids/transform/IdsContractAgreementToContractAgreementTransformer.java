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
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.Contract;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractAgreementTransformerOutput;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transforms an IDS ContractAgreement into an {@link ContractAgreement}.
 */
public class IdsContractAgreementToContractAgreementTransformer implements IdsTypeTransformer<ContractTransformerInput, ContractAgreementTransformerOutput> {

    @Override
    public Class<ContractTransformerInput> getInputType() {
        return ContractTransformerInput.class;
    }

    @Override
    public Class<ContractAgreementTransformerOutput> getOutputType() {
        return ContractAgreementTransformerOutput.class;
    }

    @Override
    public @Nullable ContractAgreementTransformerOutput transform(ContractTransformerInput object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var contractAgreement = (de.fraunhofer.iais.eis.ContractAgreement) object.getContract();
        var asset = object.getAsset();

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

        var policy = Policy.Builder.newInstance()
                .duties(edcObligations)
                .prohibitions(edcProhibitions)
                .permissions(edcPermissions)
                .build();

        var builder = ContractAgreement.Builder.newInstance()
                .policy(policy)
                .consumerAgentId(String.valueOf(contractAgreement.getConsumer()))
                .providerAgentId(String.valueOf(contractAgreement.getProvider()))
                .assetId(asset.getId());

        var idsUri = contractAgreement.getId();
        if (idsUri != null) {
            var id = IdsIdParser.parse(idsUri.toString());
            try {
                if (id.getType() != IdsType.CONTRACT_AGREEMENT) {
                    context.reportProblem("handled id is not of typ contract agreement");
                }

                builder.id(id.getValue());
            } catch (NullPointerException e) {
                context.reportProblem("cannot handle empty ids id");
            }
        }

        if (contractAgreement.getContractEnd() != null) {
            builder.contractEndDate(contractAgreement.getContractEnd().toGregorianCalendar().toZonedDateTime().toEpochSecond());
        }

        if (contractAgreement.getContractStart() != null) {
            builder.contractStartDate(contractAgreement.getContractStart().toGregorianCalendar().toZonedDateTime().toEpochSecond());
        }

        if (contractAgreement.getContractDate() != null) {
            builder.contractSigningDate(contractAgreement.getContractDate().toGregorianCalendar().toZonedDateTime().toEpochSecond());
        }

        var agreement = builder.build();
        return ContractAgreementTransformerOutput.Builder.newInstance()
                .contractAgreement(agreement)
                .policy(policy)
                .build();
    }
}

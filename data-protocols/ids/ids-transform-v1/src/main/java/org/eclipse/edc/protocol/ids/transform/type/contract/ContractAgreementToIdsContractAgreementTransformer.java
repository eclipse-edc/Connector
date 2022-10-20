/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.contract;

import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.Duty;
import de.fraunhofer.iais.eis.Permission;
import de.fraunhofer.iais.eis.Prohibition;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;

public class ContractAgreementToIdsContractAgreementTransformer implements IdsTypeTransformer<ContractAgreementRequest, de.fraunhofer.iais.eis.ContractAgreement> {

    @Override
    public Class<ContractAgreementRequest> getInputType() {
        return ContractAgreementRequest.class;
    }

    @Override
    public Class<ContractAgreement> getOutputType() {
        return ContractAgreement.class;
    }

    @Override
    public @Nullable ContractAgreement transform(ContractAgreementRequest request, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (request == null) {
            return null;
        }

        var policy = request.getPolicy();

        var idsPermissions = Optional.of(policy)
                .map(Policy::getPermissions)
                .stream().flatMap(Collection::stream)
                .map(it -> context.transform(it, Permission.class))
                .collect(Collectors.toList());

        var idsProhibitions = Optional.of(policy)
                .map(Policy::getProhibitions)
                .stream().flatMap(Collection::stream)
                .map(it -> context.transform(it, Prohibition.class))
                .collect(Collectors.toList());

        var idsObligations = Optional.of(policy)
                .map(Policy::getObligations)
                .stream().flatMap(Collection::stream)
                .map(it -> context.transform(it, Duty.class))
                .collect(Collectors.toList());

        var agreement = request.getContractAgreement(); // cannot be null
        var id = IdsId.Builder.newInstance().value(agreement.getId()).type(IdsType.CONTRACT_AGREEMENT).build().toUri();
        var builder = new ContractAgreementBuilder(id);

        builder._obligation_(idsObligations);
        builder._prohibition_(idsProhibitions);
        builder._permission_(idsPermissions);

        try {
            builder._consumer_(URI.create(agreement.getConsumerAgentId()));
        } catch (NullPointerException e) {
            context.reportProblem("cannot convert empty consumerId string to URI");
        }

        try {
            builder._provider_(URI.create(agreement.getProviderAgentId()));
        } catch (NullPointerException e) {
            context.reportProblem("cannot convert empty providerId string to URI");
        }

        try {
            builder._contractStart_(CalendarUtil.gregorianFromEpochSeconds(agreement.getContractStartDate()));
        } catch (DatatypeConfigurationException e) {
            context.reportProblem("cannot convert contract start time to XMLGregorian");
        }

        try {
            builder._contractEnd_(CalendarUtil.gregorianFromEpochSeconds(agreement.getContractEndDate()));
        } catch (DatatypeConfigurationException e) {
            context.reportProblem("cannot convert contract end time to XMLGregorian");
        }

        try {
            builder._contractDate_(CalendarUtil.gregorianFromEpochSeconds(agreement.getContractSigningDate()));
        } catch (DatatypeConfigurationException e) {
            context.reportProblem("cannot convert contract signing time to XMLGregorian");
        }

        return PropertyUtil.addPolicyPropertiesToIdsContract(policy, builder.build());
    }
}

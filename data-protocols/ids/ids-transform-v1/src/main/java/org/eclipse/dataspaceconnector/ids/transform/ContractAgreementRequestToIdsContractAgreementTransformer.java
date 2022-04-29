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
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.Duty;
import de.fraunhofer.iais.eis.Permission;
import de.fraunhofer.iais.eis.Prohibition;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;

public class ContractAgreementRequestToIdsContractAgreementTransformer  implements IdsTypeTransformer<ContractAgreementRequest, de.fraunhofer.iais.eis.ContractAgreement> {

    @Override
    public Class<ContractAgreementRequest> getInputType() {
        return ContractAgreementRequest.class;
    }

    @Override
    public Class<ContractAgreement> getOutputType() {
        return ContractAgreement.class;
    }

    @Nullable
    @Override
    public ContractAgreement transform(@Nullable ContractAgreementRequest request, @NotNull TransformerContext context) {
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

        var agreement = request.getContractAgreement();
        var idsId = IdsId.Builder.newInstance().value(agreement.getId()).type(IdsType.CONTRACT_AGREEMENT).build();
        var id = context.transform(idsId, URI.class);
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

        return builder.build();
    }
}

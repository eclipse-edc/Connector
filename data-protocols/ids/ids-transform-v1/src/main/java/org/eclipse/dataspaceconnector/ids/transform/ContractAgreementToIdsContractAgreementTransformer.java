/*
 *  Copyright (c) Fraunhofer Institute for Software and Systems Engineering
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

import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.Duty;
import de.fraunhofer.iais.eis.Permission;
import de.fraunhofer.iais.eis.Prohibition;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

public class ContractAgreementToIdsContractAgreementTransformer implements IdsTypeTransformer<ContractAgreement, de.fraunhofer.iais.eis.ContractAgreement> {

    public ContractAgreementToIdsContractAgreementTransformer() {
    }

    @Override
    public Class<ContractAgreement> getInputType() {
        return ContractAgreement.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.ContractAgreement> getOutputType() {
        return de.fraunhofer.iais.eis.ContractAgreement.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.ContractAgreement transform(ContractAgreement object,
                                                                        @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var idsPermissions = new ArrayList<Permission>();
        var idsProhibitions = new ArrayList<Prohibition>();
        var idsObligations = new ArrayList<Duty>();

        var policy = object.getPolicy();
        if (policy.getPermissions() != null) {
            for (var edcPermission : policy.getPermissions()) {
                var idsPermission = context.transform(edcPermission, Permission.class);
                idsPermissions.add(idsPermission);
            }
        }

        if (policy.getProhibitions() != null) {
            for (var edcProhibition : policy.getProhibitions()) {
                var idsProhibition = context.transform(edcProhibition, Prohibition.class);
                idsProhibitions.add(idsProhibition);
            }
        }

        if (policy.getObligations() != null) {
            for (var edcObligation : policy.getObligations()) {
                var idsObligation = context.transform(edcObligation, Duty.class);
                idsObligations.add(idsObligation);
            }
        }

        var idsId = IdsId.Builder.newInstance().value(object.getId()).type(IdsType.CONTRACT_AGREEMENT).build();
        var id = context.transform(idsId, URI.class);
        var builder = new ContractAgreementBuilder(id);

        builder._obligation_(idsObligations);
        builder._prohibition_(idsProhibitions);
        builder._permission_(idsPermissions);

        try {
            builder._consumer_(URI.create(object.getConsumerAgentId()));
        } catch (NullPointerException e) {
            context.reportProblem("cannot convert empty consumerId string to URI");
        }

        try {
            builder._provider_(URI.create(object.getProviderAgentId()));
        } catch (NullPointerException e) {
            context.reportProblem("cannot convert empty providerId string to URI");
        }

        try {
            builder._contractStart_(DatatypeFactory.newInstance().newXMLGregorianCalendar(String.valueOf(object.getContractStartDate())));
        } catch (DatatypeConfigurationException e) {
            context.reportProblem("cannot convert contract start time to XMLGregorian");
        }

        try {
            builder._contractEnd_(DatatypeFactory.newInstance().newXMLGregorianCalendar(String.valueOf(object.getContractEndDate())));
        } catch (DatatypeConfigurationException e) {
            context.reportProblem("cannot convert contract end time to XMLGregorian");
        }

        try {
            builder._contractDate_(DatatypeFactory.newInstance().newXMLGregorianCalendar(String.valueOf(object.getContractSigningDate())));
        } catch (DatatypeConfigurationException e) {
            context.reportProblem("cannot convert contract signing time to XMLGregorian");
        }

        return builder.build();
    }
}

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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.contract;

import de.fraunhofer.iais.eis.ContractOfferBuilder;
import de.fraunhofer.iais.eis.Duty;
import de.fraunhofer.iais.eis.Permission;
import de.fraunhofer.iais.eis.Prohibition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Objects;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

public class ContractOfferToIdsContractOfferTransformer implements IdsTypeTransformer<ContractOffer, de.fraunhofer.iais.eis.ContractOffer> {

    public ContractOfferToIdsContractOfferTransformer() {
    }

    @Override
    public Class<ContractOffer> getInputType() {
        return ContractOffer.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.ContractOffer> getOutputType() {
        return de.fraunhofer.iais.eis.ContractOffer.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.ContractOffer transform(@NotNull ContractOffer object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null || object.getPolicy() == null) {
            return null;
        }

        var idsPermissions = new ArrayList<Permission>();
        var idsProhibitions = new ArrayList<Prohibition>();
        var idsObligations = new ArrayList<Duty>();

        var policy = object.getPolicy();
        if (policy.getPermissions() != null) {
            for (var edcPermission : policy.getPermissions()) {
                idsPermissions.add(context.transform(edcPermission, Permission.class));
            }
        }

        if (policy.getProhibitions() != null) {
            for (var edcProhibition : policy.getProhibitions()) {
                idsProhibitions.add(context.transform(edcProhibition, Prohibition.class));
            }
        }

        if (policy.getObligations() != null) {
            for (var edcObligation : policy.getObligations()) {
                idsObligations.add(context.transform(edcObligation, Duty.class));
            }
        }

        var id = IdsId.Builder.newInstance().value(object.getId()).type(IdsType.CONTRACT_OFFER).build().toUri();
        var builder = new ContractOfferBuilder(id);

        builder._obligation_(idsObligations);
        builder._prohibition_(idsProhibitions);
        builder._permission_(idsPermissions);
        builder._consumer_(object.getConsumer());
        builder._provider_(object.getProvider());

        if (object.getContractStart() != null) {
            try {
                builder._contractStart_(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar.from(object.getContractStart()))));
            } catch (DatatypeConfigurationException e) {
                context.reportProblem("cannot convert contract start time to XMLGregorian");
            }
        }

        if (object.getContractEnd() != null) {
            try {
                builder._contractEnd_(DatatypeFactory.newInstance().newXMLGregorianCalendar(((GregorianCalendar.from(object.getContractEnd())))));
            } catch (DatatypeConfigurationException e) {
                context.reportProblem("cannot convert contract end time to XMLGregorian");
            }
        }

        return PropertyUtil.addPolicyPropertiesToIdsContract(policy, builder.build());
    }
}

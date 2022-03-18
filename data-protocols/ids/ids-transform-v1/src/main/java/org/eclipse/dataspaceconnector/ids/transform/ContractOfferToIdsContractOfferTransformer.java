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

import de.fraunhofer.iais.eis.ContractOfferBuilder;
import de.fraunhofer.iais.eis.Duty;
import de.fraunhofer.iais.eis.Permission;
import de.fraunhofer.iais.eis.Prohibition;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
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
    public @Nullable de.fraunhofer.iais.eis.ContractOffer transform(ContractOffer object, @NotNull TransformerContext context) {
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

        var idsId = IdsId.Builder.newInstance().value(object.getId()).type(IdsType.CONTRACT_OFFER).build();
        var id = context.transform(idsId, URI.class);
        ContractOfferBuilder contractOfferBuilder = new ContractOfferBuilder(id);

        contractOfferBuilder._obligation_(idsObligations);
        contractOfferBuilder._prohibition_(idsProhibitions);
        contractOfferBuilder._permission_(idsPermissions);
        contractOfferBuilder._consumer_(object.getConsumer());
        contractOfferBuilder._provider_(object.getProvider());

        if (object.getContractStart() != null) {
            try {
                contractOfferBuilder._contractStart_(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar.from(object.getContractStart()))));
            } catch (DatatypeConfigurationException e) {
                context.reportProblem("cannot convert contract start time to XMLGregorian");
            }
        }

        if (object.getContractEnd() != null) {
            try {
                contractOfferBuilder._contractEnd_(DatatypeFactory.newInstance().newXMLGregorianCalendar(((GregorianCalendar.from(object.getContractEnd())))));
            } catch (DatatypeConfigurationException e) {
                context.reportProblem("cannot convert contract end time to XMLGregorian");
            }
        }

        return contractOfferBuilder.build();
    }
}

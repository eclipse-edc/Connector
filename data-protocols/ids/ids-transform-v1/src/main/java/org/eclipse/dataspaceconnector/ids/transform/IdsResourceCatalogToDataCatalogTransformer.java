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

import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class IdsResourceCatalogToDataCatalogTransformer implements IdsTypeTransformer<ResourceCatalog, Catalog> {
    @Override
    public Class<ResourceCatalog> getInputType() {
        return ResourceCatalog.class;
    }

    @Override
    public Class<Catalog> getOutputType() {
        return Catalog.class;
    }

    @Override
    public @Nullable Catalog transform(@Nullable ResourceCatalog object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        Catalog.Builder builder = Catalog.Builder.newInstance();

        var catalogIdsId = IdsIdParser.parse(object.getId().toString());
        if (catalogIdsId.getType() != IdsType.CATALOG) {
            context.reportProblem("Catalog ID not of type catalog");
            return null;
        }

        builder.id(catalogIdsId.getValue());

        List<Resource> resources;
        if ((resources = object.getOfferedResource()) != null) {
            List<ContractOffer> contractOffers = new LinkedList<>();

            for (Resource resource : resources) {
                Asset asset = context.transform(resource, Asset.class);

                for (de.fraunhofer.iais.eis.ContractOffer offer : resource.getContractOffer()) {
                    ContractOffer contractOffer = createContractOffer(offer, asset, context);
                    contractOffers.add(contractOffer);
                }
            }

            builder.contractOffers(contractOffers);
        }

        return builder.build();
    }

    private ContractOffer createContractOffer(de.fraunhofer.iais.eis.ContractOffer object, Asset asset, TransformerContext context) {
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
                .provider(object.getProvider())
                .asset(asset);

        if (object.getId() != null) {
            var idsId = IdsIdParser.parse(object.getId().toString());
            if (idsId.getType() != IdsType.CONTRACT_OFFER) {
                context.reportProblem("Contract offer id not of type contract offer");
            } else {
                contractOfferBuilder.id(idsId.getValue());
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

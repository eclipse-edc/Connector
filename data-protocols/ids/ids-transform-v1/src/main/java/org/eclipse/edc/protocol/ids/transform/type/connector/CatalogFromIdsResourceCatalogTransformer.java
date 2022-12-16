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

package org.eclipse.edc.protocol.ids.transform.type.connector;

import de.fraunhofer.iais.eis.ResourceCatalog;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.protocol.ids.spi.transform.ContractTransformerInput;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

public class CatalogFromIdsResourceCatalogTransformer implements IdsTypeTransformer<ResourceCatalog, Catalog> {
    @Override
    public Class<ResourceCatalog> getInputType() {
        return ResourceCatalog.class;
    }

    @Override
    public Class<Catalog> getOutputType() {
        return Catalog.class;
    }

    @Override
    public @Nullable Catalog transform(@NotNull ResourceCatalog object, @NotNull TransformerContext context) {
        var builder = Catalog.Builder.newInstance();

        var result = IdsId.from(object.getId().toString());
        if (result.failed()) {
            context.reportProblem("Catalog ID is missing");
            return null;
        }

        var idsId = result.getContent();
        if (idsId.getType() != IdsType.CATALOG) {
            context.reportProblem("Catalog ID not of type catalog");
            return null;
        }
        builder.id(idsId.getValue());

        var resources = object.getOfferedResource();
        if (resources != null) {
            var contractOffers = new LinkedList<ContractOffer>();

            for (var resource : resources) {
                var asset = context.transform(resource, Asset.class);
                for (var offer : resource.getContractOffer()) {
                    var input = ContractTransformerInput.Builder.newInstance().contract(offer).asset(asset).build();
                    var contractOffer = context.transform(input, ContractOffer.class);
                    contractOffers.add(contractOffer);
                }
            }

            builder.contractOffers(contractOffers);
        }

        return builder.build();
    }
}

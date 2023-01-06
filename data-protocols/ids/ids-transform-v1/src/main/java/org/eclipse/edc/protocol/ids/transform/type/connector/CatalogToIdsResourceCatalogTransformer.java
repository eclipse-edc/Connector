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

import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResourceCatalogBuilder;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.spi.types.container.OfferedAsset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class CatalogToIdsResourceCatalogTransformer implements IdsTypeTransformer<Catalog, ResourceCatalog> {

    @Override
    public Class<Catalog> getInputType() {
        return Catalog.class;
    }

    @Override
    public Class<ResourceCatalog> getOutputType() {
        return ResourceCatalog.class;
    }

    @Override
    public @Nullable ResourceCatalog transform(@NotNull Catalog object, @NotNull TransformerContext context) {
        var id = IdsId.Builder.newInstance().value(object.getId()).type(IdsType.CATALOG).build().toUri();
        var builder = new ResourceCatalogBuilder(id);

        var resources = new ArrayList<Resource>();
        var contractOffers = object.getContractOffers();

        var distinctAssets = contractOffers.stream()
                .map(ContractOffer::getAsset)
                .distinct()
                .collect(Collectors.toList());

        for (var asset : distinctAssets) {
            var targetingOffers = contractOffers.stream()
                    .filter(c -> c.getAsset().getId().equals(asset.getId())).collect(Collectors.toList());

            var assetAndContractOffers = new OfferedAsset(asset, targetingOffers);
            var resource = context.transform(assetAndContractOffers, Resource.class);
            if (resource != null) {
                resources.add(resource);
            }
        }

        builder._offeredResource_(new ArrayList<>(resources));

        return builder.build();
    }
}

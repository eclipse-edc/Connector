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
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_ID;

public class CatalogToIdsResourceCatalogTransformer implements IdsTypeTransformer<Catalog, ResourceCatalog> {
    private final AssetIndex assetIndex;

    public CatalogToIdsResourceCatalogTransformer(AssetIndex assetIndex) {
        this.assetIndex = assetIndex;
    }

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
                .map(ContractOffer::getAssetId)
                .distinct()
                .collect(collectingAndThen(toList(), ids -> assetIndex.queryAssets(QuerySpec.Builder.newInstance()
                        .filter(new Criterion(PROPERTY_ID, "in", ids)).build()).collect(toList())));

        for (var asset : distinctAssets) {
            var targetingOffers = contractOffers.stream()
                    .filter(c -> c.getAssetId().equals(asset.getId())).collect(toList());

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

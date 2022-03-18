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
import de.fraunhofer.iais.eis.ResourceCatalogBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
    public @Nullable ResourceCatalog transform(Catalog object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        ResourceCatalogBuilder builder;
        String catalogId = object.getId();
        if (catalogId != null) {
            URI catalogIdUri = URI.create(String.join(
                    IdsIdParser.DELIMITER,
                    IdsIdParser.SCHEME,
                    IdsType.CATALOG.getValue(),
                    catalogId));
            builder = new ResourceCatalogBuilder(catalogIdUri);
        } else {
            builder = new ResourceCatalogBuilder();
        }

        List<Resource> resources = new LinkedList<>();
        List<ContractOffer> contractOffers = object.getContractOffers();

        List<Asset> distinctAssets = contractOffers.stream().map(ContractOffer::getAsset).distinct().collect(Collectors.toList());

        for (Asset distinctAsset : distinctAssets) {
            List<ContractOffer> targetingOffers = contractOffers.stream().filter(c -> c.getAsset().getId().equals(distinctAsset.getId())).collect(Collectors.toList());

            OfferedAsset assetAndContractOffers = new OfferedAsset(distinctAsset, targetingOffers);
            Resource resource = context.transform(assetAndContractOffers, Resource.class);
            if (resource != null) {
                resources.add(resource);
            }
        }

        builder._offeredResource_(new ArrayList<>(resources));

        return builder.build();
    }
}

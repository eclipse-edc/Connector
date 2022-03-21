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

import de.fraunhofer.iais.eis.ContractOffer;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class OfferedAssetToIdsResourceTransformer implements IdsTypeTransformer<OfferedAsset, Resource> {

    public OfferedAssetToIdsResourceTransformer() {
    }

    @Override
    public Class<OfferedAsset> getInputType() {
        return OfferedAsset.class;
    }

    @Override
    public Class<Resource> getOutputType() {
        return Resource.class;
    }

    @Override
    public @Nullable Resource transform(OfferedAsset object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        Representation result = context.transform(object.getAsset(), Representation.class);

        IdsId id = IdsId.Builder.newInstance()
                .value(object.getAsset().getId())
                .type(IdsType.RESOURCE)
                .build();

        URI uri = context.transform(id, URI.class);

        ResourceBuilder resourceBuilder = new ResourceBuilder(uri);

        for (var contractOffer : object.getTargetingContractOffers()) {
            ContractOffer idsOffer = context.transform(contractOffer, ContractOffer.class);
            resourceBuilder._contractOffer_(new ArrayList<>(Collections.singletonList(idsOffer)));
        }

        resourceBuilder._representation_(new ArrayList<>(Collections.singletonList(result)));

        return resourceBuilder.build();
    }
}

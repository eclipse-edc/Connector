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

package org.eclipse.dataspaceconnector.ids.transform.type.asset;

import de.fraunhofer.iais.eis.ContractOffer;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        var asset = object.getAsset();
        var result = context.transform(asset, Representation.class);
        var id = IdsId.Builder.newInstance().value(asset.getId()).type(IdsType.RESOURCE).build().toUri();

        var builder = new ResourceBuilder(id);
        for (var contractOffer : object.getTargetingContractOffers()) {
            var idsOffer = context.transform(contractOffer, ContractOffer.class);
            builder._contractOffer_(new ArrayList<>(Collections.singletonList(idsOffer)));
        }

        builder._representation_(new ArrayList<>(Collections.singletonList(result)));

        return builder.build();
    }
}

/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.asset;

import de.fraunhofer.iais.eis.ContractOffer;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.spi.types.container.OfferedAsset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

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
        if (object == null) {
            return null;
        }

        var asset = object.getAsset();
        var id = IdsId.Builder.newInstance().value(asset.getId()).type(IdsType.RESOURCE).build().toUri();

        var builder = new ResourceBuilder(id);
        var offers = object.getTargetingContractOffers().stream()
                .map(it -> context.transform(it, ContractOffer.class))
                .collect(Collectors.toList());
        builder._contractOffer_(offers);

        var representation = context.transform(asset, Representation.class);
        builder._representation_(representation);

        return builder.build();
    }
}

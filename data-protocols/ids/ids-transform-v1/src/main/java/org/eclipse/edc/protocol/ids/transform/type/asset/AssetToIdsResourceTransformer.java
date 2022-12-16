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

package org.eclipse.edc.protocol.ids.transform.type.asset;

import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;

public class AssetToIdsResourceTransformer implements IdsTypeTransformer<Asset, Resource> {

    public AssetToIdsResourceTransformer() {
    }

    @Override
    public Class<Asset> getInputType() {
        return Asset.class;
    }

    @Override
    public Class<Resource> getOutputType() {
        return Resource.class;
    }

    @Override
    public @Nullable Resource transform(@NotNull Asset object, @NotNull TransformerContext context) {
        var result = context.transform(object, Representation.class);
        var id = IdsId.Builder.newInstance().value(object.getId()).type(IdsType.RESOURCE).build().toUri();

        var builder = new ResourceBuilder(id);

        builder._representation_(new ArrayList<>(Collections.singletonList(result)));

        var description = object.getDescription();
        if (description != null) {
            builder._description_(new TypedLiteral(description));
        }

        return builder.build();
    }
}

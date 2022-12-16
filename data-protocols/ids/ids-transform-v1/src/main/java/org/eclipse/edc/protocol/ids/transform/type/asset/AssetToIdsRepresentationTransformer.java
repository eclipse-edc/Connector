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

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.CustomMediaTypeBuilder;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;

public class AssetToIdsRepresentationTransformer implements IdsTypeTransformer<Asset, Representation> {

    @Override
    public Class<Asset> getInputType() {
        return Asset.class;
    }

    @Override
    public Class<Representation> getOutputType() {
        return Representation.class;
    }

    @Override
    public @Nullable Representation transform(@NotNull Asset object, @NotNull TransformerContext context) {
        var artifact = context.transform(object, Artifact.class);
        var id = IdsId.Builder.newInstance().value(object.getId()).type(IdsType.REPRESENTATION).build().toUri();

        var builder = new RepresentationBuilder(id);
        builder._instance_(new ArrayList<>(Collections.singletonList(artifact)));

        var properties = object.getProperties();
        if (properties != null) {
            var fileType = properties.get(TransformKeys.KEY_ASSET_FILE_EXTENSION);
            if (fileType != null) {
                if (fileType instanceof String) {
                    builder._mediaType_(new CustomMediaTypeBuilder()._filenameExtension_((String) fileType).build());
                } else {
                    context.reportProblem(String.format("Asset property %s expected to be of type String", TransformKeys.KEY_ASSET_FILE_EXTENSION));
                }
            }
        }

        return builder.build();
    }
}

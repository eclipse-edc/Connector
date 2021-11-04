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

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.MediaType;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class AssetToIdsRepresentationTransformer extends AbstractAssetTransformer implements IdsTypeTransformer<Asset, Representation> {

    @Override
    public Class<Asset> getInputType() {
        return Asset.class;
    }

    @Override
    public Class<Representation> getOutputType() {
        return Representation.class;
    }

    @Override
    public @Nullable Representation transform(Asset object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        Artifact artifact = context.transform(object, Artifact.class);

        IdsId id = IdsId.Builder.newInstance()
                .value(object.getId())
                .type(IdsType.REPRESENTATION)
                .build();
        URI uri = context.transform(id, URI.class);

        RepresentationBuilder representationBuilder = new RepresentationBuilder(uri);

        var properties = object.getProperties();
        if (properties == null) {
            context.reportProblem("Asset properties null");
        } else {
            extractProperty(context, properties, TransformKeys.KEY_ASSET_FILE_EXTENSION, String.class, (value) -> {
                setMediaType(value, context, representationBuilder);
            });
        }

        representationBuilder._instance_(new ArrayList<>(Collections.singletonList(artifact)));

        return representationBuilder.build();
    }

    private static void setMediaType(@NotNull String fileExtension, TransformerContext transformerContext, RepresentationBuilder representationBuilder) {
        MediaType mediaType = transformerContext.transform(fileExtension, MediaType.class);

        if (mediaType == null) {
            return;
        }

        representationBuilder._mediaType_(mediaType);
    }
}

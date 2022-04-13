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
import de.fraunhofer.iais.eis.Representation;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Transforms an IDS Representation into an {@link Asset}.
 * Please note that, as an {@link Asset} consists of an IDS Resource, Representation and Artifact,
 * there will be some kind of information loss.
 */
public class IdsRepresentationToAssetTransformer implements IdsTypeTransformer<Representation, Asset> {

    @Override
    public Class<Representation> getInputType() {
        return Representation.class;
    }

    @Override
    public Class<Asset> getOutputType() {
        return Asset.class;
    }

    @Override
    public @Nullable Asset transform(Representation object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        IdsId idsId;
        try {
            idsId = IdsIdParser.parse(object.getId().toString());
        } catch (IllegalArgumentException e) {
            context.reportProblem(String.format("cannot read IdsId from representation (id: %s)", object.getId().toString()));
            return null;
        }

        var assetBuilder = Asset.Builder.newInstance()
                .id(idsId.getValue());

        if (object.getMediaType() != null) {
            assetBuilder.property(TransformKeys.KEY_ASSET_FILE_EXTENSION, object.getMediaType().getFilenameExtension());
        }

        // if there is only one artifact we can take some properties from there
        if (object.getInstance() != null && object.getInstance().size() == 1 &&
                object.getInstance().get(0) instanceof Artifact) {
            var artifact = (Artifact) object.getInstance().get(0);
            assetBuilder.property(TransformKeys.KEY_ASSET_FILE_NAME, artifact.getFileName());
            assetBuilder.property(TransformKeys.KEY_ASSET_BYTE_SIZE, artifact.getByteSize());
        }

        return assetBuilder.build();
    }
}

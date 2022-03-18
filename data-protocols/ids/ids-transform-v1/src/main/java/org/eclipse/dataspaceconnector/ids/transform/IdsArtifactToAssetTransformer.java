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
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IdsArtifactToAssetTransformer implements IdsTypeTransformer<Artifact, Asset> {

    @Override
    public Class<Artifact> getInputType() {
        return Artifact.class;
    }

    @Override
    public Class<Asset> getOutputType() {
        return Asset.class;
    }

    @Override
    public @Nullable Asset transform(Artifact object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        IdsId idsId;
        try {
            idsId = IdsIdParser.parse(object.getId().toString());
        } catch (IllegalArgumentException e) {
            context.reportProblem(String.format("cannot read IdsId from artifact (id: %s)", object.getId().toString()));
            return null;
        }

        var assetBuilder = Asset.Builder.newInstance()
                .id(idsId.getValue())
                .property(TransformKeys.KEY_ASSET_BYTE_SIZE, object.getByteSize())
                .property(TransformKeys.KEY_ASSET_FILE_NAME, object.getFileName());
        if (object.getProperties() != null) {
            object.getProperties().forEach(assetBuilder::property);
        }

        return assetBuilder.build();
    }
}
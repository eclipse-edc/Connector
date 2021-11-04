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
import de.fraunhofer.iais.eis.ArtifactBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.net.URI;
import java.util.Objects;

public class AssetToIdsArtifactTransformer extends AbstractAssetTransformer implements IdsTypeTransformer<Asset, Artifact> {

    @Override
    public Class<Asset> getInputType() {
        return Asset.class;
    }

    @Override
    public Class<Artifact> getOutputType() {
        return Artifact.class;
    }

    @Override
    public @Nullable Artifact transform(Asset object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        IdsId id = IdsId.Builder.newInstance()
                .value(object.getId())
                .type(IdsType.ARTIFACT)
                .build();

        URI uri = context.transform(id, URI.class);

        ArtifactBuilder artifactBuilder = new ArtifactBuilder(uri);

        var properties = object.getProperties();
        if (properties == null) {
            context.reportProblem("Asset properties null");
            return artifactBuilder.build();
        }

        extractProperty(context, properties, TransformKeys.KEY_ASSET_FILE_NAME, String.class, artifactBuilder::_fileName_);
        extractProperty(context, properties, TransformKeys.KEY_ASSET_BYTE_SIZE, BigInteger.class, artifactBuilder::_byteSize_);

        return artifactBuilder.build();
    }
}

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
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

public class AssetToIdsArtifactTransformer implements IdsTypeTransformer<Asset, Artifact> {

    @Override
    public Class<Asset> getInputType() {
        return Asset.class;
    }

    @Override
    public Class<Artifact> getOutputType() {
        return Artifact.class;
    }

    @Override
    public @Nullable Artifact transform(Asset object, @NotNull TransformerContext context) {
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

        Map<String, Object> properties = object.getProperties();
        if (properties == null) {
            return artifactBuilder.build();
        }

        Object propertyValue = properties.get(TransformKeys.KEY_ASSET_FILE_NAME);
        if (propertyValue != null) {
            if (propertyValue instanceof String) {
                artifactBuilder._fileName_((String) propertyValue);
            } else {
                context.reportProblem(String.format("Asset property %s expected to be of type %s", TransformKeys.KEY_ASSET_FILE_NAME, String.class.getName()));
            }
        }

        propertyValue = properties.get(TransformKeys.KEY_ASSET_BYTE_SIZE);
        if (propertyValue != null) {
            if (propertyValue instanceof BigInteger) {
                artifactBuilder._byteSize_((BigInteger) propertyValue);
            } else if (propertyValue instanceof Integer) {
                artifactBuilder._byteSize_(BigInteger.valueOf((int) propertyValue));
            } else if (propertyValue instanceof Long) {
                artifactBuilder._byteSize_(BigInteger.valueOf((long) propertyValue));
            } else {
                context.reportProblem(String.format("Asset property %s expected to be of type %s", TransformKeys.KEY_ASSET_BYTE_SIZE, BigInteger.class.getName()));
            }
        }
        Artifact artifact = artifactBuilder.build();
        object.getProperties().forEach(artifact::setProperty);
        return artifact;
    }
}

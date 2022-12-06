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
import de.fraunhofer.iais.eis.ArtifactBuilder;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
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

        var id = IdsId.Builder.newInstance().value(object.getId()).type(IdsType.ARTIFACT).build().toUri();
        var builder = new ArtifactBuilder(id);

        var properties = object.getProperties();
        if (properties == null) {
            return builder.build();
        }

        var fileName = properties.get(TransformKeys.KEY_ASSET_FILE_NAME);
        if (fileName != null) {
            if (fileName instanceof String) {
                builder._fileName_((String) fileName);
            } else {
                context.reportProblem(String.format("Asset property %s expected to be of type String", TransformKeys.KEY_ASSET_FILE_NAME));
            }
        }

        var byteSize = properties.get(TransformKeys.KEY_ASSET_BYTE_SIZE);
        if (byteSize != null) {
            if (byteSize instanceof BigInteger) {
                builder._byteSize_((BigInteger) byteSize);
            } else if (byteSize instanceof Integer) {
                builder._byteSize_(BigInteger.valueOf((int) byteSize));
            } else if (byteSize instanceof Long) {
                builder._byteSize_(BigInteger.valueOf((long) byteSize));
            } else {
                context.reportProblem(String.format("Asset property %s expected to be of type BigInteger, Integer or Long", TransformKeys.KEY_ASSET_BYTE_SIZE));
            }
        }

        var artifact = builder.build();

        object.getProperties().forEach(artifact::setProperty);

        return artifact;
    }
}

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
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class AssetFromIdsArtifactTransformer implements IdsTypeTransformer<Artifact, Asset> {

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

        var result = IdsId.from(object.getId());
        if (result.failed()) {
            context.reportProblem("id of incoming IDS artifact expected to be not null");
        }

        var idsId = result.getContent();
        if (idsId != null && !idsId.getType().equals(IdsType.ARTIFACT)) {
            context.reportProblem("handled object is not of type IDS artifact");
        }

        var builder = Asset.Builder.newInstance()
                .id((idsId != null && idsId.getValue() != null) ? idsId.getValue() : UUID.randomUUID().toString())
                .property(TransformKeys.KEY_ASSET_BYTE_SIZE, object.getByteSize())
                .property(TransformKeys.KEY_ASSET_FILE_NAME, object.getFileName());

        if (object.getProperties() != null) {
            object.getProperties().forEach(builder::property);
        }

        return builder.build();
    }
}

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
import de.fraunhofer.iais.eis.Representation;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Transforms an IDS Representation into an {@link Asset}.
 * Please note that, as an {@link Asset} consists of an IDS Resource, Representation and Artifact,
 * there will be some kind of information loss.
 */
public class AssetFromIdsRepresentationTransformer implements IdsTypeTransformer<Representation, Asset> {

    @Override
    public Class<Representation> getInputType() {
        return Representation.class;
    }

    @Override
    public Class<Asset> getOutputType() {
        return Asset.class;
    }

    @Override
    public @Nullable Asset transform(@NotNull Representation object, @NotNull TransformerContext context) {
        var result = IdsId.from(object.getId());
        if (result.failed()) {
            context.reportProblem("id of incoming IDS representation expected to be not null");
        }

        var idsId = result.getContent();
        if (idsId != null && !idsId.getType().equals(IdsType.REPRESENTATION)) {
            context.reportProblem("handled object is not of type IDS representation");
        }

        var builder = Asset.Builder.newInstance()
                .id((idsId != null && idsId.getValue() != null) ? idsId.getValue() : UUID.randomUUID().toString());

        if (object.getMediaType() != null) {
            builder.property(TransformKeys.KEY_ASSET_FILE_EXTENSION, object.getMediaType().getFilenameExtension());
        }

        // if there is only one artifact, we can take some properties from there
        if (object.getInstance() != null && object.getInstance().size() == 1 &&
                object.getInstance().get(0) instanceof Artifact) {
            var artifact = (Artifact) object.getInstance().get(0);

            builder.property(TransformKeys.KEY_ASSET_FILE_NAME, artifact.getFileName());
            builder.property(TransformKeys.KEY_ASSET_BYTE_SIZE, artifact.getByteSize());
        }

        return builder.build();
    }
}

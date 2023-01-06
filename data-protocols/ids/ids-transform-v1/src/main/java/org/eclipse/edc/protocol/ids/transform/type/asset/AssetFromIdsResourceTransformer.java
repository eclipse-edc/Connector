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
import de.fraunhofer.iais.eis.RepresentationInstance;
import de.fraunhofer.iais.eis.Resource;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Transforms an IDS Resource into an {@link Asset}.
 * Please note that, as an {@link Asset} consists of an IDS Resource, Representation and Artifact,
 * there may be some kind of information loss.
 */
public class AssetFromIdsResourceTransformer implements IdsTypeTransformer<Resource, Asset> {

    @Override
    public Class<Resource> getInputType() {
        return Resource.class;
    }

    @Override
    public Class<Asset> getOutputType() {
        return Asset.class;
    }

    @Override
    public @Nullable Asset transform(@NotNull Resource object, @NotNull TransformerContext context) {
        var result = IdsId.from(object.getId());
        if (result.failed()) {
            context.reportProblem("id of incoming IDS resource expected to be not null");
        }

        var idsId = result.getContent();
        if (idsId != null && !idsId.getType().equals(IdsType.RESOURCE)) {
            context.reportProblem("handled object is not of type IDS resource");
        }

        var builder = Asset.Builder.newInstance()
                .id((idsId != null && idsId.getValue() != null) ? idsId.getValue() : UUID.randomUUID().toString());

        if (object.getDescription() != null && object.getDescription().size() > 0) {
            builder.property(Asset.PROPERTY_DESCRIPTION, object.getDescription().get(0).getValue());
        }

        var representation = getRepresentationFromResource(object);
        if (representation != null) {
            if (representation.getMediaType() != null) {
                builder.property(TransformKeys.KEY_ASSET_FILE_EXTENSION, representation.getMediaType().getFilenameExtension());
            }

            var representationInstance = getInstanceFromRepresentation(representation);
            if (representationInstance instanceof Artifact) {
                var artifact = (Artifact) representation.getInstance().get(0);

                builder.property(TransformKeys.KEY_ASSET_FILE_NAME, artifact.getFileName());
                builder.property(TransformKeys.KEY_ASSET_BYTE_SIZE, artifact.getByteSize());

                if (artifact.getProperties() != null) {
                    artifact.getProperties().forEach(builder::property);
                }
            }
        }

        return builder.build();
    }

    private @Nullable Representation getRepresentationFromResource(Resource resource) {
        if (resource.getRepresentation() == null) {
            return null;
        }

        return resource.getRepresentation().stream().findFirst().orElse(null);
    }

    private @Nullable RepresentationInstance getInstanceFromRepresentation(Representation representation) {
        if (representation.getInstance() == null) {
            return null;
        }

        return representation.getInstance().stream().findFirst().orElse(null);
    }
}

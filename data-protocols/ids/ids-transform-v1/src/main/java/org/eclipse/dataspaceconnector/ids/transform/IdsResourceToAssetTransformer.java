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
import de.fraunhofer.iais.eis.RepresentationInstance;
import de.fraunhofer.iais.eis.Resource;
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
 * Transforms an IDS Resource into an {@link Asset}.
 * Please note that, as an {@link Asset} consists of an IDS Resource, Representation and Artifact,
 * there may be some kind of information loss.
 */
public class IdsResourceToAssetTransformer implements IdsTypeTransformer<Resource, Asset> {

    @Override
    public Class<Resource> getInputType() {
        return Resource.class;
    }

    @Override
    public Class<Asset> getOutputType() {
        return Asset.class;
    }

    @Override
    public @Nullable Asset transform(Resource object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        IdsId idsId;
        try {
            idsId = IdsIdParser.parse(object.getId().toString());
        } catch (IllegalArgumentException e) {
            context.reportProblem(String.format("cannot read IdsId from resource (id: %s)", object.getId().toString()));
            return null;
        }

        var assetBuilder = Asset.Builder.newInstance().id(idsId.getValue());

        if (object.getDescription() != null && object.getDescription().size() > 0) {
            assetBuilder.property(Asset.PROPERTY_DESCRIPTION, object.getDescription().get(0).getValue());
        }

        var representation = getRepresentationFromResource(object);
        if (representation != null) {
            if (representation.getMediaType() != null) {
                assetBuilder.property(TransformKeys.KEY_ASSET_FILE_EXTENSION, representation.getMediaType().getFilenameExtension());
            }

            var representationInstance = getInstanceFromRepresentation(representation);
            if (representationInstance instanceof Artifact) {
                var artifact = (Artifact) representation.getInstance().get(0);
                assetBuilder.property(TransformKeys.KEY_ASSET_FILE_NAME, artifact.getFileName());
                assetBuilder.property(TransformKeys.KEY_ASSET_BYTE_SIZE, artifact.getByteSize());
                if (artifact.getProperties() != null) {
                    artifact.getProperties().forEach(assetBuilder::property);
                }
            }
        }

        return assetBuilder.build();
    }

    private @Nullable Representation getRepresentationFromResource(Resource resource) {
        if (resource.getRepresentation() == null) {
            return null;
        }
        return resource.getRepresentation().stream()
                .findFirst()
                .orElse(null);
    }

    private @Nullable RepresentationInstance getInstanceFromRepresentation(Representation representation) {
        if (representation.getInstance() == null) {
            return null;
        }
        return representation.getInstance().stream()
                .findFirst()
                .orElse(null);
    }
}
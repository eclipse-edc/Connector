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
import de.fraunhofer.iais.eis.CustomMediaTypeBuilder;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class AssetToIdsRepresentationTransformer implements IdsTypeTransformer<Asset, Representation> {

    @Override
    public Class<Asset> getInputType() {
        return Asset.class;
    }

    @Override
    public Class<Representation> getOutputType() {
        return Representation.class;
    }

    @Override
    public @Nullable Representation transform(Asset object, @NotNull TransformerContext context) {
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
        representationBuilder._instance_(new ArrayList<>(Collections.singletonList(artifact)));

        Map<String, Object> properties = object.getProperties();
        if (properties != null) {
            Object propertyValue = properties.get(TransformKeys.KEY_ASSET_FILE_EXTENSION);
            if (propertyValue != null) {
                if (propertyValue instanceof String) {
                    representationBuilder._mediaType_(new CustomMediaTypeBuilder()._filenameExtension_((String) propertyValue).build());
                } else {
                    context.reportProblem(String.format("Asset property %s expected to be of type %s", TransformKeys.KEY_ASSET_FILE_EXTENSION, String.class.getName()));
                }
            }
        }

        return representationBuilder.build();
    }
}

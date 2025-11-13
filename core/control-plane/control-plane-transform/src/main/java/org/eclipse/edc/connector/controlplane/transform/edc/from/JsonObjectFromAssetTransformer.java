/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_CATALOG_ASSET_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromAssetTransformer extends AbstractJsonLdTransformer<Asset, JsonObject> {
    private final TypeManager typeManager;
    private final String typeContext;
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromAssetTransformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        super(Asset.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Asset asset, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, asset.getId())
                .add(TYPE, Asset.EDC_ASSET_TYPE);

        var propBuilder = jsonFactory.createObjectBuilder();
        transformProperties(asset.getProperties(), propBuilder, typeManager.getMapper(typeContext), context);
        builder.add(Asset.EDC_ASSET_PROPERTIES, propBuilder);

        if (asset.getPrivateProperties() != null && !asset.getPrivateProperties().isEmpty()) {
            var privatePropBuilder = jsonFactory.createObjectBuilder();
            transformProperties(asset.getPrivateProperties(), privatePropBuilder, typeManager.getMapper(typeContext), context);
            builder.add(Asset.EDC_ASSET_PRIVATE_PROPERTIES, privatePropBuilder);
        }

        if (asset.isCatalog()) {
            builder.add(TYPE, EDC_CATALOG_ASSET_TYPE);
        }

        if (asset.getDataAddress() != null) {
            builder.add(Asset.EDC_ASSET_DATA_ADDRESS, context.transform(asset.getDataAddress(), JsonObject.class));
        }

        if (asset.getDataplaneMetadata() != null) {
            builder.add(Asset.EDC_ASSET_DATAPLANE_METADATA, context.transform(asset.getDataplaneMetadata(), JsonObject.class));
        }

        return builder.build();
    }
}

/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.catalog.transform.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogError;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_ERROR_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;

/**
 * Transforms a {@link CatalogError} to a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromCatalogErrorTransformer extends AbstractNamespaceAwareJsonLdTransformer<CatalogError, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromCatalogErrorTransformer(JsonBuilderFactory jsonFactory) {
        this(jsonFactory, DSP_NAMESPACE_V_08);
    }

    public JsonObjectFromCatalogErrorTransformer(JsonBuilderFactory jsonFactory, JsonLdNamespace namespace) {
        super(CatalogError.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CatalogError error, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, forNamespace(DSPACE_TYPE_CATALOG_ERROR_TERM))
                .add(forNamespace(DSPACE_PROPERTY_CODE_TERM), error.getCode())
                .add(forNamespace(DSPACE_PROPERTY_REASON_TERM), jsonFactory.createArrayBuilder(error.getMessages()))
                .build();
    }
}

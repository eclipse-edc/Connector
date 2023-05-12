/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.CatalogError;
import org.eclipse.edc.protocol.dsp.spi.mapper.DspHttpStatusCodeMapper;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_ERROR;

public class JsonObjectFromCatalogErrorTransformer extends AbstractJsonLdTransformer<CatalogError, JsonObject> {

    private DspHttpStatusCodeMapper statusCodeMapper;

    public JsonObjectFromCatalogErrorTransformer(DspHttpStatusCodeMapper statusCodeMapper) {
        super(CatalogError.class, JsonObject.class);
        this.statusCodeMapper = statusCodeMapper;
    }

    @Nullable
    @Override
    public JsonObject transform(@NotNull CatalogError error, @NotNull TransformerContext context) {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, DSPACE_CATALOG_ERROR);

        var exception = error.getException();

        builder.add(DSPACE_SCHEMA + "code", String.valueOf(statusCodeMapper.mapErrorToStatusCode(exception)));

        if (exception.getMessage() != null) {
            builder.add(DSPACE_SCHEMA + "reason", Json.createArrayBuilder().add(exception.getMessage()));
        }

        return builder.build();
    }
}

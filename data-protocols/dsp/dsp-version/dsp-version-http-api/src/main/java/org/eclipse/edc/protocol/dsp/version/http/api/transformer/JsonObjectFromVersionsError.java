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

package org.eclipse.edc.protocol.dsp.version.http.api.transformer;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionsError;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_TYPE_VERSIONS_ERROR;

/**
 * Transforms a {@link VersionsError} to a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromVersionsError extends AbstractJsonLdTransformer<VersionsError, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromVersionsError(JsonBuilderFactory jsonFactory) {
        super(VersionsError.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull VersionsError error, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_VERSIONS_ERROR)
                .add(DSPACE_PROPERTY_CODE, error.getCode())
                .add(DSPACE_PROPERTY_REASON, jsonFactory.createArrayBuilder(error.getMessages()))
                .build();
    }
}

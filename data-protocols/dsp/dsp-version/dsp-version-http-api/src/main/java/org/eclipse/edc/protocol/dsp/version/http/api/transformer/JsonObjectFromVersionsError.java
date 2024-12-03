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
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;

/**
 * Transforms a {@link VersionsError} to a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromVersionsError implements TypeTransformer<VersionsError, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromVersionsError(JsonBuilderFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    @Override
    public Class<VersionsError> getInputType() {
        return VersionsError.class;
    }

    @Override
    public Class<JsonObject> getOutputType() {
        return JsonObject.class;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull VersionsError error, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(DSPACE_PROPERTY_CODE_TERM, error.getCode())
                .add(DSPACE_PROPERTY_REASON_TERM, jsonFactory.createArrayBuilder(error.getMessages()))
                .build();
    }
}

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

package org.eclipse.edc.connector.dataplane.selector.transformer;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;

import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_DEST_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_SOURCE_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.Builder;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.LAST_ACTIVE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.TURNCOUNT;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.URL;

public class JsonObjectToDataPlaneInstanceTransformer extends AbstractJsonLdTransformer<JsonObject, DataPlaneInstance> {
    public JsonObjectToDataPlaneInstanceTransformer() {
        super(JsonObject.class, DataPlaneInstance.class);
    }

    @Override
    public @Nullable DataPlaneInstance transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Builder.newInstance();
        builder.id(nodeId(jsonObject));
        visitProperties(jsonObject, (key, jsonValue) -> {
            switch (key) {
                case URL -> {
                    try {
                        builder.url(new URL(Objects.requireNonNull(transformString(jsonValue, context))));
                    } catch (MalformedURLException e) {
                        context.reportProblem(e.getMessage());
                    }
                }
                case LAST_ACTIVE -> builder.lastActive(transformInt(jsonValue, context));
                case TURNCOUNT -> builder.turnCount(transformInt(jsonValue, context));
                case ALLOWED_DEST_TYPES -> {
                    var obj = transformArray(jsonValue, Object.class, context);
                    if (obj != null && !obj.isEmpty()) {
                        builder.allowedDestTypes(Set.copyOf(obj.stream().map(Object::toString).toList()));
                    }
                }
                case ALLOWED_SOURCE_TYPES -> {
                    var obj = transformArray(jsonValue, Object.class, context);
                    if (obj != null && !obj.isEmpty()) {
                        builder.allowedSourceTypes(Set.copyOf(obj.stream().map(Object::toString).toList()));
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + key);
            }
        });

        return builder.build();
    }
}

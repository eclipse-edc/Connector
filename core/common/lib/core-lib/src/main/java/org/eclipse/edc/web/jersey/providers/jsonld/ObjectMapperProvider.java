/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.web.jersey.providers.jsonld;

import jakarta.ws.rs.ext.ContextResolver;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Provides an ObjectMapper to be used for parsing incoming requests. A custom ObjectMapper that supports the
 * Jakarta JSON API is required to allow JsonObject as a controller parameter.
 * <p>
 * Jersey's Jackson 3 provider only looks up a {@code ContextResolver<JsonMapper>}, and matches its type argument exactly.
 * A {@code ContextResolver<ObjectMapper>} would therefore be ignored, and Jersey would use its own, unconfigured mapper.
 */
public class ObjectMapperProvider implements ContextResolver<JsonMapper> {

    private final TypeManager typeManager;
    private final String typeContext;

    public ObjectMapperProvider(TypeManager typeManager, String typeContext) {
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public JsonMapper getContext(Class<?> type) {
        var mapper = typeManager.getMapper(typeContext);
        if (mapper instanceof JsonMapper jsonMapper) {
            return jsonMapper;
        }
        // returning null would let Jersey fall back to its own mapper, which ignores the configuration of the type context
        throw new EdcException("Jersey requires a %s for type context '%s', but it holds a %s"
                .formatted(JsonMapper.class.getSimpleName(), typeContext, mapper.getClass().getName()));
    }
}

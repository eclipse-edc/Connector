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

package org.eclipse.edc.web.jersey.providers.jsonld;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Provides an ObjectMapper to be used for parsing incoming requests. A custom ObjectMapper that supports the
 * Jakarta JSON API is required to allow JsonObject as a controller parameter.
 */
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

    private final TypeManager typeManager;
    private final String typeContext;

    public ObjectMapperProvider(TypeManager typeManager, String typeContext) {
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return typeManager.getMapper(typeContext);
    }
}

/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.web.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Bridges the system type manager with Jersey for de/serialization.
 */
@Provider
public class TypeManagerContextResolver implements ContextResolver<ObjectMapper> {
    private final TypeManager typeManager;

    public TypeManagerContextResolver(TypeManager typeManager) {
        this.typeManager = typeManager;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return typeManager.getMapper("json-ld");
    }
}

/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * Bridges the system type manager with Jersey for de/serialization.
 */
@Provider
public class TypeManagerContextResolver implements ContextResolver<ObjectMapper> {
    private TypeManager typeManager;

    public TypeManagerContextResolver(TypeManager typeManager) {
        this.typeManager = typeManager;
    }

    public ObjectMapper getContext(Class<?> type) {
        return typeManager.getMapper();
    }
}

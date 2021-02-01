package com.microsoft.dagx.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.types.TypeManager;
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

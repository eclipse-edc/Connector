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

package org.eclipse.edc.jsonld;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistryImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Adds support for working with JSON-LD. Provides an ObjectMapper that works with Jakarta JSON-P
 * types through the TypeManager context {@link #TYPE_MANAGER_CONTEXT_JSON_LD} and a registry
 * for {@link org.eclipse.edc.jsonld.transformer.JsonLdTransformer}s. The module also offers
 * functions for working with JSON-LD structures.
 */
@Extension(value = JsonLdExtension.NAME)
public class JsonLdExtension implements ServiceExtension {
    
    public static final String NAME = "JSON-LD Extension";
    public static final String TYPE_MANAGER_CONTEXT_JSON_LD = "json-ld";
    
    @Inject
    private TypeManager typeManager;
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Provider
    public JsonLdTransformerRegistry jsonLdTransformerRegistry() {
        return new JsonLdTransformerRegistryImpl();
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = getObjectMapper();
        typeManager.registerContext(TYPE_MANAGER_CONTEXT_JSON_LD, mapper);
    }
    
    private ObjectMapper getObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JSONPModule());
        var module = new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
            }
        };
        mapper.registerModule(module);
        return mapper;
    }
    
}

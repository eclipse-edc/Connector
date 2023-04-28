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

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformer;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Adds support for working with JSON-LD. Provides an ObjectMapper that works with Jakarta JSON-P
 * types through the TypeManager context {@link #TYPE_MANAGER_CONTEXT_JSON_LD} and a registry
 * for {@link JsonLdTransformer}s. The module also offers
 * functions for working with JSON-LD structures.
 */
@BaseExtension
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

    @Override
    public void initialize(ServiceExtensionContext context) {
        typeManager.registerContext(TYPE_MANAGER_CONTEXT_JSON_LD, JacksonJsonLd.createObjectMapper());
    }

    @Provider
    public JsonLd createJsonLdService(ServiceExtensionContext context) {
        return new TitaniumJsonLd(context.getMonitor());
    }

}

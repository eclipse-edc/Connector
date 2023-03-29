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
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

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
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class);
        return mapper;
    }
    
}

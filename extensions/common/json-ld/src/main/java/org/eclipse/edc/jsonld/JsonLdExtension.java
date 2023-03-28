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
import jakarta.json.Json;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistryImpl;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromCatalogTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToActionTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToCatalogTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToConstraintTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToDataServiceTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToDatasetTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToDistributionTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToDutyTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToPermissionTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToPolicyTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToProhibitionTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Map;

@Extension(value = JsonLdExtension.NAME)
@Provides({JsonLdTransformerRegistry.class})
public class JsonLdExtension implements ServiceExtension {
    
    public static final String NAME = "JSON-LD Extension";
    
    @Inject
    private TypeManager typeManager;
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = getObjectMapper();
        typeManager.registerContext("json-ld", mapper);
        
        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());
        
        var registry = new JsonLdTransformerRegistryImpl();
        
        // EDC model to JSON-LD transformers
        registry.register(new JsonObjectFromCatalogTransformer(jsonBuilderFactory, mapper));
        registry.register(new JsonObjectFromDatasetTransformer(jsonBuilderFactory, mapper));
        registry.register(new JsonObjectFromPolicyTransformer(jsonBuilderFactory, mapper));
        registry.register(new JsonObjectFromDistributionTransformer(jsonBuilderFactory, mapper));
        registry.register(new JsonObjectFromDataServiceTransformer(jsonBuilderFactory, mapper));
        
        // JSON-LD to EDC model transformers
        registry.register(new JsonObjectToCatalogTransformer());
        registry.register(new JsonObjectToDataServiceTransformer());
        registry.register(new JsonObjectToDatasetTransformer());
        registry.register(new JsonObjectToDistributionTransformer());
        registry.register(new JsonObjectToPolicyTransformer());
        registry.register(new JsonObjectToPermissionTransformer());
        registry.register(new JsonObjectToProhibitionTransformer());
        registry.register(new JsonObjectToDutyTransformer());
        registry.register(new JsonObjectToActionTransformer());
        registry.register(new JsonObjectToConstraintTransformer());
        registry.register(new JsonValueToGenericTypeTransformer(mapper));
        
        context.registerService(JsonLdTransformerRegistry.class, registry);
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

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

package org.eclipse.edc.jsonld;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistryImpl;
import org.eclipse.edc.jsonld.transformer.from.FromContractOfferTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromCatalogTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToCatalogTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToDataServiceTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.jsonld.transformer.to.ToContractOfferTransformer;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.transformer.Namespaces.ODRL_SCHEMA;

class BindingTest {
    public static final String FOO_NAMESPACE = "https://foo.com/schema.json/";

    private ObjectMapper mapper;
    private JsonLdTransformerRegistryImpl registry = new JsonLdTransformerRegistryImpl();
    private JsonBuilderFactory builderFactory;

    @Test
    public void toCatalog() throws Exception {
        var json = mapper.readValue(getClass().getClassLoader().getResource("catalog.message.json"), JsonObject.class);

        var expanded = JsonLd.expand(JsonDocument.of(json)).options(new JsonLdOptions((url, options) -> createDefaultDocumentLoader())).get();
        var result = registry.transform(expanded.getJsonObject(0), Catalog.class);
    }

    @Test
    public void fromCatalog() throws Exception {
        var catalog = Catalog.Builder.newInstance()
                .dataset(Dataset.Builder.newInstance().id(UUID.randomUUID().toString()).offer("offerId", createPolicy()).build())
                .dataset(Dataset.Builder.newInstance().id(UUID.randomUUID().toString()).offer("offerId", Policy.Builder.newInstance().build()).build())
                .property(FOO_NAMESPACE + "fooprop", "fooval")
                .property(FOO_NAMESPACE + "bazprop", "bazval")
                .build();

        var result = registry.transform(catalog, JsonObject.class);

        var jsonObject = result.getContent();

        var document = JsonDocument.of(jsonObject);
        var contextObject = builderFactory.createObjectBuilder()
                .add(DCAT_PREFIX, DCAT_SCHEMA)
                .add(ODRL_PREFIX, ODRL_SCHEMA)
                .build();
        var contextDocument = JsonDocument.of(builderFactory.createObjectBuilder().add("@context", contextObject).build());

        var compacted = JsonLd.compact(document, contextDocument).get();
    }

    @Test
    void compactAndWriteCatalog() throws Exception {
        var catalog = mapper.readValue(getClass().getClassLoader().getResource("catalog.message.json"), Object.class);
        var jsonObject = mapper.convertValue(catalog, JsonObject.class);
        var document = JsonDocument.of(jsonObject);
        var compacted = JsonLd.compact(document, document).get();
    }

    @Test
    void serializeObject() throws JsonProcessingException {
        var objectBuilder = builderFactory.createObjectBuilder();
        var object = objectBuilder.add("foo", "bar").build();
        var result = mapper.writeValueAsString(object);
    }

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JSONPModule());
        var module = new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
            }
        };
        mapper.registerModule(module);
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class);

        registry = new JsonLdTransformerRegistryImpl();

        var toGenericTypeTransformer = new JsonValueToGenericTypeTransformer(mapper);
        registry.register(toGenericTypeTransformer);

        var toCatalogTransformer = new JsonObjectToCatalogTransformer();
        registry.register(toCatalogTransformer);

        var toContractOfferTransformer = new ToContractOfferTransformer();
        registry.register(toContractOfferTransformer);

        var toDataServiceTransformer = new JsonObjectToDataServiceTransformer();
        registry.register(toDataServiceTransformer);

        builderFactory = Json.createBuilderFactory(Map.of());

        var fromCatalogTransformer = new JsonObjectFromCatalogTransformer(builderFactory, mapper);
        registry.register(fromCatalogTransformer);

        var fromContractOfferTransformer = new FromContractOfferTransformer(builderFactory);
        registry.register(fromContractOfferTransformer);

        var fromPolicyTransformer = new JsonObjectFromPolicyTransformer(builderFactory, mapper);
        registry.register(fromPolicyTransformer);
    }

    private static Policy createPolicy() {
        var atomicConstraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("Use"))
                .rightExpression(new LiteralExpression(true))
                .build();
        return Policy.Builder.newInstance()
                .permissions(List.of(Permission.Builder.newInstance().constraint(atomicConstraint).build()))
                .build();
    }

    @NotNull
    private JsonDocument createDefaultDocumentLoader() throws JsonLdError {
        try {
            return JsonDocument.of(getClass().getClassLoader().getResource("ids.context.json").openStream());
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

}



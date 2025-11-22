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

package org.eclipse.edc.iam.decentralizedclaims.transform.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.transform.TestContextProvider;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TransformerContextImpl;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToPresentationQueryMessageTransformerTest {
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final TypeTransformerRegistry trr = new TypeTransformerRegistryImpl();
    private final TransformerContext context = new TransformerContextImpl(trr);


    @BeforeEach
    void setUp() {
        trr.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_withScopes(TestContextProvider.TestContext ctx) throws JsonProcessingException {

        var transformer = new JsonObjectToPresentationQueryTransformer(typeManager, "test", ctx.namespace());

        var obj = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                    "%s"
                  ],
                  "@type": "PresentationQueryMessage",
                  "scope": [
                    "org.eclipse.edc.vc.type:TestCredential:read",
                    "org.eclipse.edc.vc.type:AnotherCredential:all"
                  ]
                }
                """.formatted(ctx.context());
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = ctx.jsonLd().expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getScopes()).hasSize(2)
                .containsExactlyInAnyOrder(
                        "org.eclipse.edc.vc.type:TestCredential:read",
                        "org.eclipse.edc.vc.type:AnotherCredential:all");
        assertThat(query.getPresentationDefinition()).isNull();
    }


    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_withEmptyScopes(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationQueryTransformer(typeManager, "test", ctx.namespace());

        var obj = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                     "%s"
                  ],
                  "@type": "PresentationQueryMessage",
                  "scope": []
                }
                """.formatted(ctx.context());
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = ctx.jsonLd().expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getScopes()).isEmpty();
        assertThat(query.getPresentationDefinition()).isNull();
    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_withNullScopes(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationQueryTransformer(typeManager, "test", ctx.namespace());

        var obj = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                     "%s"
                  ],
                  "@type": "PresentationQueryMessage"
                }
                """.formatted(ctx.context());
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = ctx.jsonLd().expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getScopes()).isEmpty();
        assertThat(query.getPresentationDefinition()).isNull();
    }


    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_withScopes_separatedByWhitespace(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationQueryTransformer(typeManager, "test", ctx.namespace());

        var obj = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                     "%s"
                  ],
                  "@type": "PresentationQueryMessage",
                  "scope": [
                    "org.eclipse.edc.vc.type:TestCredential:read org.eclipse.edc.vc.type:AnotherCredential:all"
                  ]
                }
                """.formatted(ctx.context());
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = ctx.jsonLd().expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getScopes()).hasSize(2)
                .containsExactlyInAnyOrder(
                        "org.eclipse.edc.vc.type:TestCredential:read",
                        "org.eclipse.edc.vc.type:AnotherCredential:all");
        assertThat(query.getPresentationDefinition()).isNull();
    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_withPresentationDefinition(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationQueryTransformer(typeManager, "test", ctx.namespace());
        var json = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                     "%s"
                  ],
                  "@type": "PresentationQueryMessage",
                   "presentationDefinition": {
                       "id": "first simple example",
                       "input_descriptors": [
                         {
                           "id": "descriptor-id-1",
                           "name": "A specific type of VC",
                           "purpose": "We want a VC of this type",
                           "constraints": {
                             "fields": [
                               {
                                 "path": [
                                   "$.type"
                                 ],
                                 "filter": {
                                   "type": "string",
                                   "pattern": "<the type of VC e.g. degree certificate>"
                                 }
                               }
                             ]
                           }
                         }
                       ]
                     }
                }
                """.formatted(ctx.context());
        var jobj = mapper.readValue(json, JsonObject.class);

        var expansion = ctx.jsonLd().expand(jobj);
        assertThat(expansion.succeeded()).withFailMessage(expansion::getFailureDetail).isTrue();

        var query = transformer.transform(expansion.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getScopes()).isNotNull().isEmpty();
        assertThat(query.getPresentationDefinition()).isNotNull();
        assertThat(query.getPresentationDefinition().getInputDescriptors()).isNotEmpty()
                .allSatisfy(id -> assertThat(id.getId()).isEqualTo("descriptor-id-1"));

    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_withScopesAndPresDef(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationQueryTransformer(typeManager, "test", ctx.namespace());
        var json = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                     "%s"
                  ],
                  "@type": "PresentationQueryMessage",
                  "scope": ["test-scope1"],
                   "presentationDefinition": {
                       "id": "first simple example",
                       "input_descriptors": [
                         {
                           "id": "descriptor-id-1",
                           "name": "A specific type of VC",
                           "purpose": "We want a VC of this type",
                           "constraints": {
                             "fields": [
                               {
                                 "path": [
                                   "$.type"
                                 ],
                                 "filter": {
                                   "type": "string",
                                   "pattern": "<the type of VC e.g. degree certificate>"
                                 }
                               }
                             ]
                           }
                         }
                       ]
                     }
                }
                """.formatted(ctx.context());
        var jobj = mapper.readValue(json, JsonObject.class);

        var expansion = ctx.jsonLd().expand(jobj);
        assertThat(expansion.succeeded()).withFailMessage(expansion::getFailureDetail).isTrue();

        var query = transformer.transform(expansion.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getScopes()).isNotNull().containsExactly("test-scope1");
        assertThat(query.getPresentationDefinition()).isNotNull();
        assertThat(query.getPresentationDefinition().getInputDescriptors()).isNotEmpty()
                .allSatisfy(id -> assertThat(id.getId()).isEqualTo("descriptor-id-1"));
    }
}

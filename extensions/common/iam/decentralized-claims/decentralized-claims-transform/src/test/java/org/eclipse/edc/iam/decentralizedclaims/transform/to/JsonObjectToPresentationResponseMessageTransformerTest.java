/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToPresentationResponseMessageTransformerTest {
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
    void transform(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationResponseMessageTransformer(typeManager, "test", ctx.namespace());
        var obj = """
                {
                  "@context": [
                    "%s"
                  ],
                  "@type": "PresentationResponseMessage",
                  "presentation": "jwtPresentation"
                }
                """.formatted(ctx.context());
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = ctx.jsonLd().expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(1)
                .containsExactly("jwtPresentation");
        assertThat(query.getPresentationSubmission()).isNull();
    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_MultipleJwt(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationResponseMessageTransformer(typeManager, "test", ctx.namespace());
        var obj = """
                {
                  "@context": [
                    "%s"
                  ],
                  "@type": "PresentationResponseMessage",
                  "presentation": ["firstJwtPresentation", "secondJwtPresentation"]
                }
                """.formatted(ctx.context());
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = ctx.jsonLd().expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(2)
                .containsExactly("firstJwtPresentation", "secondJwtPresentation");
        assertThat(query.getPresentationSubmission()).isNull();
    }


    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_singleJson(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationResponseMessageTransformer(typeManager, "test", ctx.namespace());

        var obj = """
                {
                      "@context": [
                           "%s"
                      ],
                      "@type": "PresentationResponseMessage",
                      "presentation": {
                          "@context": [
                              "https://www.w3.org/2018/credentials/v1"
                          ],
                          "type": [
                              "VerifiablePresentation"
                          ]
                      }
                  }
                """.formatted(ctx.context());
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = ctx.jsonLd().expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(1)
                .allMatch((val) -> val instanceof Map);

        assertThat(query.getPresentationSubmission()).isNull();
    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_multipleJson(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationResponseMessageTransformer(typeManager, "test", ctx.namespace());

        var obj = """
                {
                         "@context": [
                              "%s"
                         ],
                         "@type": "PresentationResponseMessage",
                         "presentation": [
                             {
                                 "@context": [
                                     "https://www.w3.org/2018/credentials/v1"
                                 ],
                                 "type": [
                                     "VerifiablePresentation"
                                 ]
                             },
                             {
                                 "@context": [
                                     "https://www.w3.org/2018/credentials/v1"
                                 ],
                                 "type": [
                                     "VerifiablePresentation"
                                 ]
                             }
                         ]
                     }
                """.formatted(ctx.context());
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = ctx.jsonLd().expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(2)
                .allMatch((val) -> val instanceof Map);


        assertThat(query.getPresentationSubmission()).isNull();
    }

    @ParameterizedTest
    @ArgumentsSource(TestContextProvider.class)
    void transform_mixed(TestContextProvider.TestContext ctx) throws JsonProcessingException {
        var transformer = new JsonObjectToPresentationResponseMessageTransformer(typeManager, "test", ctx.namespace());

        var obj = """
                {
                         "@context": [
                              "%s"
                         ],
                         "@type": "PresentationResponseMessage",
                         "presentation": [
                             {
                                 "@context": [
                                     "https://www.w3.org/2018/credentials/v1"
                                 ],
                                 "type": [
                                     "VerifiablePresentation"
                                 ]
                             },
                             "jwtPresentation"
                         ]
                     }
                """.formatted(ctx.context());
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = ctx.jsonLd().expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(2)
                .allMatch((val) -> val instanceof Map || val instanceof String);


        assertThat(query.getPresentationSubmission()).isNull();
    }

}

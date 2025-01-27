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

package org.eclipse.edc.iam.identitytrust.transform.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TransformerContextImpl;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DCP_CONTEXT_URL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToPresentationResponseMessageTransformerTest {
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonObjectToPresentationResponseMessageTransformer transformer = new JsonObjectToPresentationResponseMessageTransformer(typeManager, "test");
    private final JsonLd jsonLd = new TitaniumJsonLd(mock());
    private final TypeTransformerRegistry trr = new TypeTransformerRegistryImpl();
    private final TransformerContext context = new TransformerContextImpl(trr);

    @BeforeEach
    void setUp() {
        jsonLd.registerCachedDocument("https://identity.foundation/presentation-exchange/submission/v1", TestUtils.getFileFromResourceName("presentation_ex.json").toURI());
        jsonLd.registerCachedDocument(DCP_CONTEXT_URL, TestUtils.getFileFromResourceName("document/dcp.v08.jsonld").toURI());
        // delegate to the generic transformer

        trr.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }

    @Test
    void transform() throws JsonProcessingException {
        var obj = """
                {
                  "@context": [
                    "https://w3id.org/tractusx-trust/v0.8"
                  ],
                  "@type": "PresentationResponseMessage",
                  "presentation": "jwtPresentation"
                }
                """;
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = jsonLd.expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(1)
                .containsExactly("jwtPresentation");
        assertThat(query.getPresentationSubmission()).isNull();
    }

    @Test
    void transform_MultipleJwt() throws JsonProcessingException {
        var obj = """
                {
                  "@context": [
                    "https://w3id.org/tractusx-trust/v0.8"
                  ],
                  "@type": "PresentationResponseMessage",
                  "presentation": ["firstJwtPresentation", "secondJwtPresentation"]
                }
                """;
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = jsonLd.expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(2)
                .containsExactly("firstJwtPresentation", "secondJwtPresentation");
        assertThat(query.getPresentationSubmission()).isNull();
    }


    @Test
    void transform_singleJson() throws JsonProcessingException {
        var obj = """
                {
                      "@context": [
                          "https://w3id.org/tractusx-trust/v0.8"
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
                """;
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = jsonLd.expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(1)
                .allMatch((val) -> val instanceof Map);

        assertThat(query.getPresentationSubmission()).isNull();
    }

    @Test
    void transform_multipleJson() throws JsonProcessingException {
        var obj = """
                {
                         "@context": [
                             "https://w3id.org/tractusx-trust/v0.8"
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
                """;
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = jsonLd.expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(2)
                .allMatch((val) -> val instanceof Map);


        assertThat(query.getPresentationSubmission()).isNull();
    }

    @Test
    void transform_mixed() throws JsonProcessingException {
        var obj = """
                {
                         "@context": [
                             "https://w3id.org/tractusx-trust/v0.8"
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
                """;
        var json = mapper.readValue(obj, JsonObject.class);
        var jo = jsonLd.expand(json);
        assertThat(jo.succeeded()).withFailMessage(jo::getFailureDetail).isTrue();

        var query = transformer.transform(jo.getContent(), context);
        assertThat(query).isNotNull();
        assertThat(query.getPresentation()).hasSize(2)
                .allMatch((val) -> val instanceof Map || val instanceof String);


        assertThat(query.getPresentationSubmission()).isNull();
    }

}

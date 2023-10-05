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

package org.eclipse.edc.iam.identitytrust.transform.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.core.transform.TransformerContextImpl;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.core.transform.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.transform.TestConstants.EXAMPLE_VC_JSONLD;
import static org.mockito.Mockito.mock;

class JsonObjectToVerifiableCredentialTransformerTest {
    public static final ObjectMapper OBJECT_MAPPER = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLdService = new TitaniumJsonLd(mock());
    private TransformerContext context;
    private JsonObjectToVerifiableCredentialTransformer transformer;

    @BeforeEach
    void setUp() throws URISyntaxException {
        transformer = new JsonObjectToVerifiableCredentialTransformer(OBJECT_MAPPER, jsonLdService);
        var registry = new TypeTransformerRegistryImpl();
        registry.register(new JsonObjectToCredentialSubjectTransformer());
        registry.register(new JsonObjectToCredentialStatusTransformer());
        registry.register(new JsonValueToGenericTypeTransformer(OBJECT_MAPPER));
        registry.register(transformer);

        context = new TransformerContextImpl(registry);
        jsonLdService.registerCachedDocument("https://www.w3.org/ns/credentials/v2", Thread.currentThread().getContextClassLoader().getResource("document/credentials.v2.jsonld").toURI());
    }

    @Test
    void transform() throws JsonProcessingException {

        var jsonObj = OBJECT_MAPPER.readValue(EXAMPLE_VC_JSONLD, JsonObject.class);
        var vc = transformer.transform(jsonObj, context);

        assertThat(vc).isNotNull();
        assertThat(vc.getCredentialSubject()).isNotNull().hasSize(1);
        assertThat(vc.getTypes()).hasSize(2);
        assertThat(vc.getDescription()).isNotNull();
        assertThat(vc.getName()).isNotNull();
        assertThat(vc.getCredentialStatus()).isNull();
        assertThat(vc.getRawVc()).isNotNull().isEqualToIgnoringWhitespace(EXAMPLE_VC_JSONLD);
    }
}
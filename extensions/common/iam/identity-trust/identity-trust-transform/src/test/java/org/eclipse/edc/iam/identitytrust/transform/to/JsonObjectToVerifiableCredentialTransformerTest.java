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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TransformerContextImpl;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.transform.TestData.EXAMPLE_VC_JSONLD;
import static org.eclipse.edc.iam.identitytrust.transform.TestData.EXAMPLE_VC_JSONLD_ISSUER_IS_URL;
import static org.eclipse.edc.iam.identitytrust.transform.TestData.EXAMPLE_VC_SUB_IS_ARRAY_JSONLD;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToVerifiableCredentialTransformerTest {
    public static final ObjectMapper OBJECT_MAPPER = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonLd jsonLdService = new TitaniumJsonLd(mock());
    private TransformerContext context;
    private JsonObjectToVerifiableCredentialTransformer transformer;

    @BeforeEach
    void setUp() throws URISyntaxException {
        transformer = new JsonObjectToVerifiableCredentialTransformer();
        var registry = new TypeTransformerRegistryImpl();
        registry.register(new JsonObjectToCredentialSubjectTransformer());
        registry.register(new JsonObjectToCredentialStatusTransformer());
        registry.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        registry.register(new JsonObjectToIssuerTransformer());
        registry.register(transformer);

        context = spy(new TransformerContextImpl(registry));
        jsonLdService.registerCachedDocument("https://www.w3.org/2018/credentials/v2", Thread.currentThread().getContextClassLoader().getResource("document/credentials.v2.jsonld").toURI());
        when(typeManager.getMapper("test")).thenReturn(OBJECT_MAPPER);
    }

    @Test
    void transform() throws JsonProcessingException {

        var jsonObj = OBJECT_MAPPER.readValue(EXAMPLE_VC_JSONLD, JsonObject.class);
        var vc = transformer.transform(jsonLdService.expand(jsonObj).getContent(), context);

        assertThat(vc).isNotNull();
        assertThat(vc.getCredentialSubject()).isNotNull().hasSize(1);
        assertThat(vc.getType()).hasSize(2);
        assertThat(vc.getDescription()).isNotNull();
        assertThat(vc.getName()).isNotNull();
        assertThat(vc.getCredentialStatus()).isNotNull();
        assertThat(vc.getIssuer()).isNotNull().extracting(Issuer::id).isEqualTo("https://university.example/issuers/565049");
        assertThat(vc.getIssuanceDate().isBefore(vc.getExpirationDate())).isTrue();
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_credentialSubjectIsArray() throws JsonProcessingException {

        var jsonObj = OBJECT_MAPPER.readValue(EXAMPLE_VC_SUB_IS_ARRAY_JSONLD, JsonObject.class);
        var vc = transformer.transform(jsonLdService.expand(jsonObj).getContent(), context);

        assertThat(vc).isNotNull();
        assertThat(vc.getCredentialSubject()).isNotNull().hasSize(2);
        assertThat(vc.getCredentialStatus()).hasSize(2);
        assertThat(vc.getType()).hasSize(2);
        assertThat(vc.getDescription()).isNotNull();
        assertThat(vc.getName()).isNotNull();
        assertThat(vc.getCredentialStatus()).isNotNull();
        assertThat(vc.getIssuer()).isNotNull().extracting(Issuer::id).isEqualTo("https://university.example/issuers/565049");
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_issuerIsUrl() throws JsonProcessingException {

        var jsonObj = OBJECT_MAPPER.readValue(EXAMPLE_VC_JSONLD_ISSUER_IS_URL, JsonObject.class);
        var vc = transformer.transform(jsonLdService.expand(jsonObj).getContent(), context);

        assertThat(vc).isNotNull();
        assertThat(vc.getCredentialSubject()).isNotNull().hasSize(1);
        assertThat(vc.getType()).hasSize(2);
        assertThat(vc.getDescription()).isNotNull();
        assertThat(vc.getName()).isNotNull();
        assertThat(vc.getCredentialStatus()).isNotNull();
        assertThat(vc.getIssuer()).isNotNull().extracting(Issuer::id).isEqualTo("https://university.example/issuers/565049");
        verify(context, never()).reportProblem(anyString());
    }
}

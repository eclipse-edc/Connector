/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.spi.TrustedIssuer.TRUSTED_ISSUER_SUPPORTED_TYPES_IRI;
import static org.eclipse.edc.protocol.spi.TrustedIssuer.TRUSTED_ISSUER_TYPE_IRI;
import static org.mockito.Mockito.mock;

class JsonObjectFromTrustedIssuerTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectFromTrustedIssuerTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromTrustedIssuerTransformer(jsonFactory);
    }

    @Test
    void transform_shouldConvertAllFields() {
        var issuer = TrustedIssuer.Builder.newInstance()
                .id("did:web:trusted.issuer")
                .supportedTypes(List.of("MembershipCredential", "OnboardingCredential"))
                .build();

        var result = transformer.transform(issuer, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("did:web:trusted.issuer");
        assertThat(result.getString(TYPE)).isEqualTo(TRUSTED_ISSUER_TYPE_IRI);
        var types = result.getJsonArray(TRUSTED_ISSUER_SUPPORTED_TYPES_IRI);
        assertThat(types).hasSize(2);
        assertThat(types.getString(0)).isEqualTo("MembershipCredential");
        assertThat(types.getString(1)).isEqualTo("OnboardingCredential");
    }

    @Test
    void transform_shouldProduceEmptySupportedTypes_whenNoneProvided() {
        var issuer = TrustedIssuer.Builder.newInstance()
                .id("did:web:trusted.issuer")
                .build();

        var result = transformer.transform(issuer, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonArray(TRUSTED_ISSUER_SUPPORTED_TYPES_IRI)).isEmpty();
    }
}

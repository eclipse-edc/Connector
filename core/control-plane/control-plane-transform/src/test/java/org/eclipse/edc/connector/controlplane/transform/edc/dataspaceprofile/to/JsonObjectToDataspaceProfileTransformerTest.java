/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from.JsonObjectFromDataspaceProfileTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from.JsonObjectFromTrustedIssuerTransformer;
import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToDataspaceProfileTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectToDataspaceProfileTransformer transformer;
    private JsonObjectFromDataspaceProfileTransformer fromTransformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToDataspaceProfileTransformer();
        fromTransformer = new JsonObjectFromDataspaceProfileTransformer(jsonFactory);
    }

    @Test
    void transform_roundTrip() {
        var profile = DataspaceProfile.Builder.newInstance()
                .name("profile-name")
                .protocolVersion("2025-1")
                .path("/profile-name")
                .binding("HTTPS")
                .namespace("https://example.com/ns/")
                .jsonLdContextsUrl(List.of("https://example.com/ctx/v1", "https://example.com/ctx/v2"))
                .build();

        var json = fromTransformer.transform(profile, context);
        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("profile-name");
        assertThat(result.getProtocolVersion()).isEqualTo("2025-1");
        assertThat(result.getPath()).isEqualTo("/profile-name");
        assertThat(result.getBinding()).isEqualTo("HTTPS");
        assertThat(result.getNamespace()).isEqualTo("https://example.com/ns/");
        assertThat(result.getJsonLdContextsUrl())
                .containsExactly("https://example.com/ctx/v1", "https://example.com/ctx/v2");
    }

    @Test
    void transform_roundTrip_withTrustedIssuers() {
        var fromTrustedIssuerTransformer = new JsonObjectFromTrustedIssuerTransformer(jsonFactory);
        var toTrustedIssuerTransformer = new JsonObjectToTrustedIssuerTransformer();
        when(context.transform(any(TrustedIssuer.class), eq(JsonObject.class)))
                .thenAnswer(inv -> fromTrustedIssuerTransformer.transform(inv.getArgument(0), context));
        when(context.transform(any(JsonObject.class), eq(TrustedIssuer.class)))
                .thenAnswer(inv -> toTrustedIssuerTransformer.transform(inv.getArgument(0), context));

        var profile = DataspaceProfile.Builder.newInstance()
                .name("profile-name")
                .protocolVersion("2025-1")
                .path("/profile-name")
                .binding("HTTPS")
                .namespace("https://example.com/ns/")
                .trustedIssuers(List.of(
                        TrustedIssuer.Builder.newInstance()
                                .id("did:web:trusted.issuer")
                                .supportedTypes(List.of("MembershipCredential"))
                                .build()
                ))
                .build();

        var json = fromTransformer.transform(profile, context);
        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getTrustedIssuers()).hasSize(1);
        assertThat(result.getTrustedIssuers().get(0).getId()).isEqualTo("did:web:trusted.issuer");
        assertThat(result.getTrustedIssuers().get(0).getSupportedTypes()).containsExactly("MembershipCredential");
    }
}

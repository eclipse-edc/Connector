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

package org.eclipse.edc.iam.verifiablecredentials.rules;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class HasValidIssuerTest {

    private static final String ISSUER = "did:web:issuer1";

    @Test
    void shouldSucceed_whenTrustedIssuerWithExactType() {
        var credential = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1", "type2"))
                .build();
        var trustedIssuer = TrustedIssuer.Builder.newInstance().id(ISSUER).supportedTypes(List.of("type1", "type3")).build();
        var rule = new HasValidIssuer(createProfile(List.of(trustedIssuer)));

        var result = rule.apply(credential);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldSucceed_whenIssuerSupportsWildcard() {
        var vc = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1", "type2"))
                .build();
        var trustedIssuer = TrustedIssuer.Builder.newInstance().id(ISSUER).supportedTypes(List.of("*")).build();
        var rule = new HasValidIssuer(createProfile(List.of(trustedIssuer)));

        assertThat(rule.apply(vc)).isSucceeded();
    }

    @Test
    void shouldFail_whenIssuerNotTrusted() {
        var vc = createCredentialBuilder()
                .issuer(new Issuer("untrustedIssuer", Map.of()))
                .types(List.of("type1", "type2"))
                .build();
        var trustedIssuer = TrustedIssuer.Builder.newInstance().id(ISSUER).supportedTypes(List.of("type1", "type2")).build();
        var rule = new HasValidIssuer(createProfile(List.of(trustedIssuer)));

        var result = rule.apply(vc);

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenIssuerTypesMismatch() {
        var vc = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1"))
                .build();
        var trustedIssuer = TrustedIssuer.Builder.newInstance().id(ISSUER).supportedTypes(List.of("type2", "type3")).build();
        var rule = new HasValidIssuer(createProfile(List.of(trustedIssuer)));

        assertThat(rule.apply(vc))
                .isFailed()
                .detail()
                .isEqualTo("Credential types '[type1]' are not supported for issuer '%s'".formatted(ISSUER));
    }

    private DataspaceProfileContext createProfile(List<TrustedIssuer> trustedIssuers) {
        return new DataspaceProfileContext("any", new ProtocolVersion("any", "any", "any"), mock(), mock(),
                new JsonLdNamespace("https://example.org/ns/"), List.of(), trustedIssuers);
    }

}
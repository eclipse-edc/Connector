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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class HasValidIssuerTest {

    private static final String ISSUER = "did:web:issuer1";

    @Test
    void shouldSucceed_whenAllTypesSupported() {
        var credential = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1", "type2"))
                .build();
        var rule = new HasValidIssuer(createProfile(trustedIssuer("type1", "type2", "type3")));

        assertThat(rule.apply(credential)).isSucceeded();
    }

    @Test
    void shouldFail_whenNotAllTypesSupported() {
        var credential = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1", "type2"))
                .build();
        var rule = new HasValidIssuer(createProfile(trustedIssuer("type1", "type3")));

        assertThat(rule.apply(credential))
                .isFailed()
                .detail()
                .isEqualTo("Credential types '[type1, type2]' are not supported for issuer '%s' (unsupported: [type2])".formatted(ISSUER));
    }

    @Test
    void shouldSucceed_whenNotAllTypesSupported_andMatchAnyType() {
        var credential = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1", "type2"))
                .build();
        var rule = new HasValidIssuer(createProfile(trustedIssuer("type1", "type3")), true);

        assertThat(rule.apply(credential)).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {"VerifiableCredential", VC_PREFIX + "VerifiableCredential"})
    void shouldIgnoreBaseType(String baseType) {
        var credential = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of(baseType, "type1"))
                .build();
        var rule = new HasValidIssuer(createProfile(trustedIssuer("type1")));

        assertThat(rule.apply(credential)).isSucceeded();
    }

    @Test
    void shouldFail_whenOnlyBaseTypeSupported() {
        var credential = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("VerifiableCredential", "type1"))
                .build();
        var rule = new HasValidIssuer(createProfile(trustedIssuer("VerifiableCredential")));

        assertThat(rule.apply(credential)).isFailed().detail().contains("(unsupported: [type1])");
    }

    @Test
    void shouldSucceed_whenCredentialHasOnlyBaseType_andBaseTypeSupported() {
        var credential = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("VerifiableCredential"))
                .build();
        var rule = new HasValidIssuer(createProfile(trustedIssuer("VerifiableCredential")));

        assertThat(rule.apply(credential)).isSucceeded();
    }

    @Test
    void shouldFail_whenCredentialHasOnlyBaseType_andBaseTypeNotSupported() {
        var credential = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("VerifiableCredential"))
                .build();
        var rule = new HasValidIssuer(createProfile(trustedIssuer("type1")));

        assertThat(rule.apply(credential)).isFailed();
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
                .isEqualTo("Credential types '[type1]' are not supported for issuer '%s' (unsupported: [type1])".formatted(ISSUER));
    }

    @Test
    void shouldFail_whenIssuerTypesMismatch_andMatchAnyType() {
        var vc = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1"))
                .build();
        var rule = new HasValidIssuer(createProfile(trustedIssuer("type2", "type3")), true);

        assertThat(rule.apply(vc))
                .isFailed()
                .detail()
                .isEqualTo("Credential types '[type1]' are not supported for issuer '%s'".formatted(ISSUER));
    }

    private List<TrustedIssuer> trustedIssuer(String... supportedTypes) {
        return List.of(TrustedIssuer.Builder.newInstance().id(ISSUER).supportedTypes(List.of(supportedTypes)).build());
    }

    private DataspaceProfileContext createProfile(List<TrustedIssuer> trustedIssuers) {
        return new DataspaceProfileContext("any", new ProtocolVersion("any", "any", "any"), mock(), mock(),
                new JsonLdNamespace("https://example.org/ns/"), List.of(), trustedIssuers);
    }

}
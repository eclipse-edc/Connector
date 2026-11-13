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

package org.eclipse.edc.verifiablecredentials.jwt.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EnvelopedCredentialsRuleTest {

    private static final String CONTENT_TYPE = "data:application/vc+jwt,";

    private final Function<String, Result<Void>> credentialVerifier = mock();
    private final EnvelopedCredentialsRule rule = new EnvelopedCredentialsRule(credentialVerifier);

    @BeforeEach
    void setUp() {
        when(credentialVerifier.apply(any())).thenReturn(Result.success());
    }

    @Test
    void shouldSucceed_whenCredentialIsValid() {
        var result = rule.checkRule(token(List.of(envelopedCredential(CONTENT_TYPE + "jwt1"))), Map.of());

        assertThat(result).isSucceeded();
        verify(credentialVerifier).apply("jwt1");
    }

    @Test
    void shouldVerifyEveryCredential_whenEnvelopeContainsMany() {
        var result = rule.checkRule(token(List.of(envelopedCredential(CONTENT_TYPE + "jwt1;" + CONTENT_TYPE + "jwt2"))), Map.of());

        assertThat(result).isSucceeded();
        verify(credentialVerifier).apply("jwt1");
        verify(credentialVerifier).apply("jwt2");
    }

    @Test
    void shouldSucceed_whenNoCredentials() {
        var result = rule.checkRule(token(List.of()), Map.of());

        assertThat(result).isSucceeded();
        verifyNoInteractions(credentialVerifier);
    }

    @Test
    void shouldFail_andVerifyAllCredentials_whenOneIsInvalid() {
        when(credentialVerifier.apply("jwt1")).thenReturn(Result.failure("invalid-1"));
        var credentials = List.<Object>of(envelopedCredential(CONTENT_TYPE + "jwt1"), envelopedCredential(CONTENT_TYPE + "jwt2"));

        var result = rule.checkRule(token(credentials), Map.of());

        assertThat(result).isFailed().detail().contains("invalid-1");
        verify(credentialVerifier).apply("jwt2");
    }

    @Test
    void shouldFail_whenClaimIsMissing() {
        var result = rule.checkRule(ClaimToken.Builder.newInstance().build(), Map.of());

        assertThat(result).isFailed().detail().isEqualTo("No verifiable credential found in 'verifiableCredential' claim");
    }

    @Test
    void shouldFail_whenClaimIsNotList() {
        var token = ClaimToken.Builder.newInstance().claim("verifiableCredential", envelopedCredential(CONTENT_TYPE + "jwt1")).build();

        var result = rule.checkRule(token, Map.of());

        assertThat(result).isFailed().detail().isEqualTo("No verifiable credential found in 'verifiableCredential' claim");
        verifyNoInteractions(credentialVerifier);
    }

    @Test
    void shouldFail_whenCredentialHasWrongType() {
        var result = rule.checkRule(token(List.of(Map.of("type", "VerifiableCredential", "id", CONTENT_TYPE + "jwt1"))), Map.of());

        assertThat(result).isFailed().detail()
                .isEqualTo("Incorrect 'type' field in verifiable credential. Must be 'EnvelopedVerifiableCredential' but was 'VerifiableCredential'");
        verifyNoInteractions(credentialVerifier);
    }

    @Test
    void shouldFail_whenCredentialIsNotAnObject() {
        var result = rule.checkRule(token(List.of(CONTENT_TYPE + "jwt1")), Map.of());

        assertThat(result).isFailed().detail().contains("Incorrect 'type' field in verifiable credential");
        verifyNoInteractions(credentialVerifier);
    }

    @Test
    void shouldFail_whenIdIsNotString() {
        var result = rule.checkRule(token(List.of(Map.of("type", "EnvelopedVerifiableCredential", "id", 12345))), Map.of());

        assertThat(result).isFailed().detail().isEqualTo("No enveloped credential found in 'id' claim");
    }

    @ParameterizedTest
    @ValueSource(strings = { "jwt1", "data:application/vp+jwt,jwt1", "data:application/vc+jwt;jwt1" })
    void shouldFail_whenEnvelopeHasIncorrectPrefix(String envelope) {
        var result = rule.checkRule(token(List.of(envelopedCredential(envelope))), Map.of());

        assertThat(result).isFailed().detail()
                .isEqualTo("Incorrect credential envelope. Must start with 'data:application/vc+jwt,' but it did not.");
        verifyNoInteractions(credentialVerifier);
    }

    private ClaimToken token(List<Object> credentials) {
        return ClaimToken.Builder.newInstance().claim("verifiableCredential", credentials).build();
    }

    private Map<String, Object> envelopedCredential(String id) {
        return Map.of("type", "EnvelopedVerifiableCredential", "id", id);
    }
}

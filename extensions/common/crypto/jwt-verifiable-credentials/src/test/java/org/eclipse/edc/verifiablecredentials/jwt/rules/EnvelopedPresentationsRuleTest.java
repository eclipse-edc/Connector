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

import java.util.Map;
import java.util.function.BiFunction;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EnvelopedPresentationsRuleTest {

    private static final String CONTENT_TYPE = "data:application/vp+jwt,";

    private final BiFunction<String, ClaimToken, Result<Void>> presentationVerifier = mock();
    private final EnvelopedPresentationsRule rule = new EnvelopedPresentationsRule(presentationVerifier);

    @BeforeEach
    void setUp() {
        when(presentationVerifier.apply(any(), any())).thenReturn(Result.success());
    }

    @Test
    void shouldSucceed_whenPresentationIsValid() {
        var envelope = envelope(CONTENT_TYPE + "jwt1");

        var result = rule.checkRule(envelope, Map.of());

        assertThat(result).isSucceeded();
        verify(presentationVerifier).apply("jwt1", envelope);
    }

    @Test
    void shouldVerifyEveryPresentation_whenEnvelopeContainsMany() {
        var envelope = envelope(CONTENT_TYPE + "jwt1;" + CONTENT_TYPE + "jwt2");

        var result = rule.checkRule(envelope, Map.of());

        assertThat(result).isSucceeded();
        verify(presentationVerifier).apply("jwt1", envelope);
        verify(presentationVerifier).apply("jwt2", envelope);
    }

    @Test
    void shouldFail_andVerifyAllPresentations_whenOneIsInvalid() {
        when(presentationVerifier.apply(eq("jwt1"), any())).thenReturn(Result.failure("invalid-1"));
        var envelope = envelope(CONTENT_TYPE + "jwt1;" + CONTENT_TYPE + "jwt2");

        var result = rule.checkRule(envelope, Map.of());

        assertThat(result).isFailed().detail().contains("invalid-1");
        verify(presentationVerifier).apply("jwt2", envelope);
    }

    @Test
    void shouldFail_whenIdIsMissing() {
        var result = rule.checkRule(ClaimToken.Builder.newInstance().build(), Map.of());

        assertThat(result).isFailed().detail().isEqualTo("No enveloped presentation found in 'id' claim");
        verifyNoInteractions(presentationVerifier);
    }

    @Test
    void shouldFail_whenIdIsNotString() {
        var result = rule.checkRule(ClaimToken.Builder.newInstance().claim("id", 12345).build(), Map.of());

        assertThat(result).isFailed().detail().isEqualTo("No enveloped presentation found in 'id' claim");
        verifyNoInteractions(presentationVerifier);
    }

    @ParameterizedTest
    @ValueSource(strings = { "jwt1", "data:application/vc+jwt,jwt1", "data:application/vp+jwt;jwt1" })
    void shouldFail_whenEnvelopeHasIncorrectPrefix(String id) {
        var result = rule.checkRule(envelope(id), Map.of());

        assertThat(result).isFailed().detail()
                .isEqualTo("Incorrect presentation envelope. Must start with 'data:application/vp+jwt,' but it did not.");
        verifyNoInteractions(presentationVerifier);
    }

    private ClaimToken envelope(String id) {
        return ClaimToken.Builder.newInstance().claim("id", id).header("kid", "did:web:holder#key-1").build();
    }
}

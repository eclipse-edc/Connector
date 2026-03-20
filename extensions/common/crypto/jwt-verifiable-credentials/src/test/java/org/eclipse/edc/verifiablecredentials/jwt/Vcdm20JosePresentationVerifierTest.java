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

package org.eclipse.edc.verifiablecredentials.jwt;

import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Vcdm20JosePresentationVerifierTest {
    private final TokenValidationService tokenValidationService = mock();
    private final PublicKeyResolver publicKeyResolver = mock();
    private final Vcdm20JosePresentationVerifier verifier = new Vcdm20JosePresentationVerifier(tokenValidationService, publicKeyResolver);

    @BeforeEach
    void setUp() {
        when(tokenValidationService.validate(anyString(), any())).thenReturn(Result.success(
                ClaimToken.Builder.newInstance().build()
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = { TestConstants.VP_SIMPLE_JOSE_ENVELOPED_CREDENTIAL, TestConstants.VP_ENVELOPED_JOSE_ENVELOPED_CREDENTIAL })
    void canHandle(String rawJose) {
        assertThat(verifier.canHandle(rawJose)).isTrue();
    }

    @Test
    void canHandle_vcdm11() {
        assertThat(verifier.canHandle(TestConstants.VP_EXAMPLE_VCDM11)).isFalse();
    }

    @Test
    void canHandle_notVpToken() {
        assertThat(verifier.canHandle(TestConstants.RANDOM_JWT)).isFalse();
    }

    @Test
    void canHandle_notJwt() {
        assertThat(verifier.canHandle("not a jwt")).isFalse();
    }


    @Test
    void verify_enveloped() {
        assertThat(verifier.verify(TestConstants.VP_ENVELOPED_JOSE_ENVELOPED_CREDENTIAL, mock())).isSucceeded();
    }

    @Test
    void verify_simple() {
        assertThat(verifier.verify(TestConstants.VP_SIMPLE_JOSE_ENVELOPED_CREDENTIAL, mock())).isSucceeded();

    }

    @Test
    void verify_randomJwt_expectFailure() {
        assertThat(verifier.verify(TestConstants.RANDOM_JWT, mock())).isFailed()
                .detail().contains("Not a valid VP token - missing the 'type' claim");
    }

    @Test
    void verify_tokenValidationFailed_expectFailure() {
        when(tokenValidationService.validate(anyString(), any())).thenReturn(Result.failure("test-failure"));
        assertThat(verifier.verify(TestConstants.VP_ENVELOPED_JOSE_ENVELOPED_CREDENTIAL, mock())).isFailed()
                .detail().contains("test-failure");
    }

    @ParameterizedTest
    @ValueSource(strings = { TestConstants.VP_SIMPLE_JOSE_ENVELOPED_CREDENTIAL, TestConstants.VP_ENVELOPED_JOSE_ENVELOPED_CREDENTIAL })
    void verify_multipleVc_oneInvalid(String vpToken) {
        when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class)))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().build())) // VP token
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().build())) // first VC
                .thenReturn(Result.failure("test-failure")); // second VC

        assertThat(verifier.verify(vpToken, mock())).isFailed()
                .detail().contains("test-failure");
    }

    @Test
    void canHandle_unknownType() {
        assertThat(verifier.canHandle(jwt("{\"type\":\"UnknownType\"}"))).isFalse();
    }

    @Test
    void verify_unknownType_expectFailure() {
        assertThat(verifier.verify(jwt("{\"type\":\"UnknownType\"}"), mock())).isFailed()
                .detail().contains("unknown type 'UnknownType'");
    }

    @Test
    void verify_envelopedPresentation_missingId_expectFailure() {
        assertThat(verifier.verify(jwt("{\"type\":\"EnvelopedVerifiablePresentation\"}"), mock())).isFailed()
                .detail().contains("No enveloped credential found in 'id' claim");
    }

    @Test
    void verify_envelopedPresentation_incorrectPrefix_expectFailure() {
        var token = jwt("{\"type\":\"EnvelopedVerifiablePresentation\",\"id\":\"wrongprefix,somedata\"}");
        assertThat(verifier.verify(token, mock())).isFailed()
                .detail().contains("Incorrect presentation envelope");
    }

    @Test
    void verify_vpToken_wrongCredentialType_expectFailure() {
        var token = jwt("{\"type\":\"VerifiablePresentation\",\"verifiableCredential\":[{\"type\":\"WrongType\",\"id\":\"data:application/vc+jwt,x\"}]}");
        assertThat(verifier.verify(token, mock())).isFailed()
                .detail().contains("Incorrect 'type' field in verifiable credential");
    }

    @Test
    void verify_vpToken_nonStringCredentialId_expectFailure() {
        var token = jwt("{\"type\":\"VerifiablePresentation\",\"verifiableCredential\":[{\"type\":\"EnvelopedVerifiableCredential\",\"id\":12345}]}");
        assertThat(verifier.verify(token, mock())).isFailed()
                .detail().contains("No enveloped credential found in 'id' claim");
    }

    @Test
    void verify_vpToken_emptyCredentialList() {
        var token = jwt("{\"type\":\"VerifiablePresentation\",\"verifiableCredential\":[]}");
        assertThat(verifier.verify(token, mock())).isSucceeded();
    }

    @Test
    void verify_credentialEnvelope_incorrectPrefix_expectFailure() {
        var token = jwt("{\"type\":\"VerifiablePresentation\",\"verifiableCredential\":[{\"type\":\"EnvelopedVerifiableCredential\",\"id\":\"wrongprefix,x\"}]}");
        assertThat(verifier.verify(token, mock())).isFailed()
                .detail().contains("Incorrect credential envelope");
    }

    private static String jwt(String payloadJson) {
        var header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        var payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".AAAA";
    }
}

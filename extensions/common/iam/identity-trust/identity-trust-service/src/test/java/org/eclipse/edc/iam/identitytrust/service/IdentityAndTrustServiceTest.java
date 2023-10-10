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

package org.eclipse.edc.iam.identitytrust.service;


import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityAndTrustServiceTest {
    private final SecureTokenService mockedSts = mock();
    private final IdentityAndTrustService service = new IdentityAndTrustService(mockedSts, "did:web:test", mock());


    @Nested
    class VerifyObtainToken {
        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "org.eclipse.edc:TestCredential:modify", "org.eclipse.edc:TestCredential:", "org.eclipse.edc:TestCredential: ", "org.eclipse.edc:TestCredential:write*", ":TestCredential:read",
                "org.eclipse.edc:fooCredential:+" })
        void obtainClientCredentials_invalidScopeString(String scope) {
            var tp = TokenParameters.Builder.newInstance()
                    .scope(scope)
                    .audience("test-audience")
                    .build();
            AbstractResultAssert.assertThat(service.obtainClientCredentials(tp))
                    .isNotNull()
                    .isFailed()
                    .detail().contains("Scope string invalid");
        }

        @ParameterizedTest(name = "Scope: {0}")
        @ValueSource(strings = { "org.eclipse.edc:TestCredential:modify", "org.eclipse.edc:TestCredential:", "org.eclipse.edc:TestCredential: ", "org.eclipse.edc:TestCredential:write*", ":TestCredential:read",
                "org.eclipse.edc:fooCredential:+" })
        @NullSource
        @EmptySource
        void obtainClientCredentials_validScopeString(String scope) {
            var tp = TokenParameters.Builder.newInstance()
                    .scope(scope)
                    .audience("test-audience")
                    .build();
            AbstractResultAssert.assertThat(service.obtainClientCredentials(tp))
                    .isNotNull()
                    .isFailed()
                    .detail().contains("Scope string invalid");
        }


        @Test
        void obtainClientCredentials_stsFails() {
            var scope = "org.eclipse.edc.vp.type:TestCredential:read";
            var tp = TokenParameters.Builder.newInstance()
                    .scope(scope)
                    .audience("test-audience")
                    .build();
            when(mockedSts.createToken(any(), any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
            AbstractResultAssert.assertThat(service.obtainClientCredentials(tp)).isSucceeded();
            verify(mockedSts).createToken(argThat(m -> m.get("iss").equals("did:web:test") &&
                    m.get("sub").equals("did:web:test") &&
                    m.get("aud").equals(tp.getAudience())), eq(scope));
        }
    }

    @Nested
    class VerifyVpValidation {

    }

}
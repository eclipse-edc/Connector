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

package org.eclipse.edc.api.iam.identitytrust.sts.validation;

import org.eclipse.edc.api.iam.identitytrust.sts.model.StsTokenRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;


public class StsTokenRequestValidatorTest {

    public static final String AUDIENCE = "aud";
    public static final String CLIENT_SECRET = "secret";
    public static final String TOKEN = "token";
    public static final String SCOPES = "scopes";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    private final StsTokenRequestValidator validator = new StsTokenRequestValidator();

    @ParameterizedTest
    @ArgumentsSource(StsTokenRequestArgumentProvider.class)
    void validate_failure_withMissingParameters(StsTokenRequest request) {
        assertThat(validator.validate(request)).isFailed();
    }

    @Test
    void validate() {

        var request = StsTokenRequest.Builder.newInstance()
                .audience(AUDIENCE)
                .clientSecret(CLIENT_SECRET)
                .clientId("client_id")
                .grantType(CLIENT_CREDENTIALS)
                .build();
        assertThat(validator.validate(request)).isSucceeded();
    }

    static class StsTokenRequestArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            return Stream.of(
                    Arguments.of(StsTokenRequest.Builder.newInstance().accessToken(TOKEN).bearerAccessScope(SCOPES).build()),
                    Arguments.of(StsTokenRequest.Builder.newInstance().audience(AUDIENCE).clientSecret(CLIENT_SECRET).grantType(CLIENT_CREDENTIALS).build()),
                    Arguments.of(StsTokenRequest.Builder.newInstance().clientId(CLIENT_ID).clientSecret(CLIENT_SECRET).grantType(CLIENT_CREDENTIALS).build()),
                    Arguments.of(StsTokenRequest.Builder.newInstance().audience(AUDIENCE).clientId(CLIENT_ID).grantType(CLIENT_CREDENTIALS).build()),
                    Arguments.of(StsTokenRequest.Builder.newInstance().audience(AUDIENCE).clientId(CLIENT_ID).clientSecret(CLIENT_SECRET).build())
            );
        }
    }

}

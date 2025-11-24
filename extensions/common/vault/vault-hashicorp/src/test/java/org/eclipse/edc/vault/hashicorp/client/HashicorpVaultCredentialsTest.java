/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.vault.hashicorp.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashicorpVaultCredentialsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serDes() throws JsonProcessingException {
        var credentials = HashicorpVaultCredentials.Builder.newInstance()
                .token("token")
                .build();

        assertThat(mapper.writeValueAsString(credentials)).isNotNull();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidConfiguration.class)
    void testBuilder(String token, String clientId, String clientSecret, String tokenUrl) {
        assertThatThrownBy(() -> HashicorpVaultCredentials.Builder.newInstance()
                .token(token)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tokenUrl(tokenUrl)
                .build())
                .isInstanceOf(IllegalArgumentException.class);

    }

    private static class InvalidConfiguration implements ArgumentsProvider {
        @Override
        public @NotNull Stream<? extends Arguments> provideArguments(@NotNull ParameterDeclarations parameters,
                                                                     @NotNull ExtensionContext context) {
            return Stream.of(
                    Arguments.of("token", "clientId", "clientSecret", "tokenUrl"),
                    Arguments.of("", "clientId", "clientSecret", "tokenUrl"),
                    Arguments.of(null, null, "clientSecret", "tokenUrl"),
                    Arguments.of(null, "clientId", null, "tokenUrl"),
                    Arguments.of(null, "clientId", "clientSecret", null),
                    Arguments.of("token", "", "clientSecret", "tokenUrl"),
                    Arguments.of("token", "clientId", "", "tokenUrl"),
                    Arguments.of("token", "clientId", "clientSecret", ""),
                    Arguments.of("token", null, "clientSecret", "tokenUrl"),
                    Arguments.of("token", "clientId", null, "tokenUrl"),
                    Arguments.of("token", "clientId", "clientSecret", null),
                    Arguments.of("token", "clientId", null, null)
            );
        }

    }
}
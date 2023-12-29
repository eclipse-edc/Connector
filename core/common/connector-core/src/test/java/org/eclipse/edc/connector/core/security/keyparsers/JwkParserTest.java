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

package org.eclipse.edc.connector.core.security.keyparsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.security.PrivateKey;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.core.security.keyparsers.KeyFunctions.createEc;
import static org.eclipse.edc.connector.core.security.keyparsers.KeyFunctions.createOkp;
import static org.eclipse.edc.connector.core.security.keyparsers.KeyFunctions.createRsa;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class JwkParserTest {

    private final JwkParser parser = new JwkParser(new ObjectMapper(), mock());

    @Test
    void canHandle() {

    }

    @ParameterizedTest
    @ArgumentsSource(KeyProvider.class)
    void parse(JWK jwk) {
        var result = parser.parse(jwk.toJSONString());
        assertThat(result).isSucceeded().isInstanceOf(PrivateKey.class);
    }

    @ParameterizedTest
    @ArgumentsSource(KeyProvider.class)
    void parse_noPrivateKey(JWK jwk) {
        var publickey = jwk.toPublicJWK();
        var result = parser.parse(publickey.toJSONString());
        assertThat(result).isFailed().detail().isEqualTo("The provided key material was in JWK format, but did not contain any (readable) private key");
    }

    private static class KeyProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(Named.named("EC-P256", createEc(Curve.P_256))),
                    Arguments.of(Named.named("EC-P384", createEc(Curve.P_384))),
                    Arguments.of(Named.named("EC-P521", createEc(Curve.P_521))),
                    Arguments.of(Named.named("EC-secp256k1", createEc(Curve.SECP256K1))),
                    Arguments.of(Named.named("OKP-Ed25519", createOkp(Curve.Ed25519))),
                    Arguments.of(Named.named("OKP-X25519", createOkp(Curve.X25519))),
                    Arguments.of(Named.named("RSA-2048", createRsa(2048))),
                    Arguments.of(Named.named("RSA-4096", createRsa(4096)))
            );
        }


    }
}
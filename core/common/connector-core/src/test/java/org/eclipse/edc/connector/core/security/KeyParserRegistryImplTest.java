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

package org.eclipse.edc.connector.core.security;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.KeyParser;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

class KeyParserRegistryImplTest {

    private final KeyParserRegistryImpl registry = new KeyParserRegistryImpl();

    @Test
    void parse() {
        var encoded = "encoded-private-key";
        registry.register(new KeyParser() {
            @Override
            public boolean canHandle(String encoded) {
                return true;
            }

            @Override
            public Result<PrivateKey> parse(String encoded) {
                return success(rsaKey().getPrivate());
            }
        });

        assertThat(registry.parse(encoded)).isSucceeded();
    }

    @Test
    void parse_noParser() {
        assertThat(registry.parse("test-private-key")).isFailed()
                .detail().isEqualTo("No parser found that can handle that format.");
    }

    @Test
    void parse_parserThrowsError() {
        registry.register(new KeyParser() {
            @Override
            public boolean canHandle(String encoded) {
                return true;
            }

            @Override
            public Result<PrivateKey> parse(String encoded) {
                return failure("test-failure");
            }
        });

        assertThat(registry.parse("test-private-key"))
                .isFailed()
                .detail()
                .isEqualTo("test-failure");
    }

    private KeyPair rsaKey() {
        try {
            var gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }
}
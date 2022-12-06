/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.core.security;

import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DefaultPrivateKeyParseFunctionTest {


    private DefaultPrivateKeyParseFunction parseFunction;

    @BeforeEach
    public void setUp() {
        parseFunction = new DefaultPrivateKeyParseFunction();
    }

    @Test
    void verifyParseInvalidPemThrowsException() {
        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> parseFunction.apply(UUID.randomUUID().toString()))
                .withMessageContaining("Object cannot be null");
    }

    @ParameterizedTest(name = "{index} {1}")
    @CsvSource({ "rsa-privatekey.pem, RSA", "ec-privatekey.pem, EC" })
    void verifyParseSuccess(String keyFileName, String expectedAlgo) throws IOException {
        var pem = loadResourceFile(keyFileName);

        var key = parseFunction.apply(pem);

        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo(expectedAlgo);
    }

    /**
     * Load content from a resource file.
     */
    private String loadResourceFile(String file) throws IOException {
        return new String(
                Objects.requireNonNull(
                                DefaultPrivateKeyParseFunctionTest.class.getClassLoader().getResourceAsStream(file)
                        )
                        .readAllBytes());
    }
}
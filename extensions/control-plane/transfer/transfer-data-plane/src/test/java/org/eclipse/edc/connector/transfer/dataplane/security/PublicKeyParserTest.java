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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PublicKeyParserTest {

    @ParameterizedTest(name = "{index} {1}")
    @CsvSource({ "rsa-pubkey.pem, RSA", "ec-pubkey.pem, EC" })
    void resolveKey(String keyFileName, String expectedAlgo) throws IOException {
        var pem = loadResourceFile(keyFileName);

        var key = PublicKeyParser.from(pem);
        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo(expectedAlgo);
    }

    /**
     * Load content from a resource file.
     */
    private static String loadResourceFile(String file) throws IOException {
        return new String(
                Objects.requireNonNull(
                                PublicKeyParserTest.class.getClassLoader().getResourceAsStream(file)
                        )
                        .readAllBytes());
    }
}
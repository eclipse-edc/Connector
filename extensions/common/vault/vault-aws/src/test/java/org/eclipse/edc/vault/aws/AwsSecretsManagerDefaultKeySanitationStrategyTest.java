/*
 *  Copyright (c) 2023 Amazon Web Services
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amazon Web Services - Initial Implementation
 *
 */

package org.eclipse.edc.vault.aws;

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


class AwsSecretsManagerDefaultKeySanitationStrategyTest {

    private final Monitor monitor = mock(Monitor.class);

    private final AwsSecretsManagerVaultSanitationStrategy sanitizer =
            new AwsSecretsManagerVaultDefaultSanitationStrategy(monitor);

    @Test
    void resolveSecret_sanitizeKeyNameReplacesInvalidCharacters() {
        var key2 = "invalid#key";

        var sanitized = sanitizer.sanitizeKey(key2);

        assertThat(sanitized).isEqualTo("invalid-key" + "_" + key2.hashCode());
    }

    @Test
    void resolveSecret_sanitizeKeyNameDoesNotReplaceValidCharacters() {
        var sanitizer = new AwsSecretsManagerVaultDefaultSanitationStrategy(monitor);
        for (var validCharacter : List.of('_', '+', '-', '@', '/', '.')) {
            var validKey = "valid" + validCharacter + "key";

            assertThat(sanitizer.sanitizeKey(validKey)).isEqualTo(validKey + '_' + validKey.hashCode());
        }
    }

    @Test
    void resolveSecret_sanitizeKeyNameLimitsKeySize() {
        var key = "-".repeat(10000);

        var sanitized = sanitizer.sanitizeKey(key);

        assertThat(sanitized)
                .isEqualTo("-".repeat(500) + "_" + key.hashCode());
        assertThat(sanitized.length()).isEqualTo(512);
    }

    @Test
    void resolveSecret_sanitizeKeyNameLimitsKeySize2() {
        var key = "-".repeat(500);

        var sanitized = sanitizer.sanitizeKey(key);

        assertThat(sanitized)
                .isEqualTo("-".repeat(500) + "_" + key.hashCode());
        assertThat(sanitized.length()).isLessThanOrEqualTo(512);
    }

}
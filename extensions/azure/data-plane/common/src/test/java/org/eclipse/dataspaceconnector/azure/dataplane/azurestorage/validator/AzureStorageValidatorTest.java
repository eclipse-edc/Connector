/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AzureStorageValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"abc", "abcdefghijabcdefghijbcde", "1er", "451", "ge45"})
    void validateAccountName_success(String input) {
        AzureStorageValidator.validateAccountName(input);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "$log", "r", "re", "a a", " b", "ag_c", "re-r", "bdjfkCJdfd", "efer:a",
            "abcdefghijabcdefghijbcdef"})
    void validateAccountName_fail(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AzureStorageValidator.validateAccountName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "$root", "$logs", "$web",
            "re-r", "z0r-a-q",
            "abc", "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz01234567890", "1er", "451", "ge45"})
    void validateContainerName_success(String input) {
        AzureStorageValidator.validateContainerName(input);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"$log", "  ", "r", "re", "a a", "-ree", "era-", "z0rr--", " b", "ag_c",
            "bdjfkCJdfd", "efer:a", "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz012345678901"})
    void validateContainerName_fail(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AzureStorageValidator.validateContainerName(input));
    }

    @ParameterizedTest
    @MethodSource("validBlobNames")
    void validateBlobName_success(String input) {
        AzureStorageValidator.validateBlobName(input);
    }

    private static Stream<String> validBlobNames() {
        return Stream.of(
                "geq",
                "Qja143",
                "ABE",
                "a name",
                "end space ",
                "je`~3j4k%$':\\",
                "abcdefghijklmnop".repeat(64),
                "a/b".repeat(253));
    }

    private static Stream<String> invalidBlobNames() {
        return Stream.of(
                null,
                "",
                "abcdefghijklmnop".repeat(64) + "a",
                "a/b".repeat(254));
    }


    @ParameterizedTest
    @MethodSource("invalidBlobNames")
    void validateBlobName_fail(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AzureStorageValidator.validateBlobName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"YQo=", "YWJjZGVmZ2hpamtsbW5hCg=="})
    void validateSharedKey_success(String input) {
        AzureStorageValidator.validateSharedKey(input);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "YWJjZGVmZ2hpamtsbW5hCg="})
    void validateSharedKey_fail(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AzureStorageValidator.validateSharedKey(input));
    }
}
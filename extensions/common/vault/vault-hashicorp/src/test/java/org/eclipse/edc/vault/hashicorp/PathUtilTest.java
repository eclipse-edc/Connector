/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilTest {


    @ParameterizedTest
    @ArgumentsSource(CorrectPathsProvider.class)
    void trimsPathsCorrect(String path, String expected) {
        final String result = PathUtil.trimLeadingOrEndingSlash(path);

        assertThat(expected).isEqualTo(result);
    }

    private static class CorrectPathsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("v1/secret/data", "v1/secret/data"),
                    Arguments.of("/v1/secret/data", "v1/secret/data"),
                    Arguments.of("/v1/secret/data/", "v1/secret/data"),
                    Arguments.of("v1/secret/data/", "v1/secret/data"));
        }
    }
}

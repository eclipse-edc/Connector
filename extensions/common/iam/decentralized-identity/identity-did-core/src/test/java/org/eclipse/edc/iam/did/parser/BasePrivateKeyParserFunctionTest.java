/*
 *  Copyright (c) 2023 Amadeus
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

package org.eclipse.edc.iam.did.parser;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

abstract class BasePrivateKeyParserFunctionTest<KEY_TYPE> {

    private final Function<String, KEY_TYPE> function;
    private final String fileName;

    protected BasePrivateKeyParserFunctionTest(Function<String, KEY_TYPE> function, String fileName) {
        this.function = function;
        this.fileName = fileName;
    }

    @Test
    public void parse() throws IOException {
        try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            Objects.requireNonNull(is, "Failed to open file: " + fileName);
            var pem = new String(is.readAllBytes());
            var key = function.apply(pem);
            assertThat(key).satisfies(verify());
        }
    }

    protected abstract ThrowingConsumer<? super KEY_TYPE> verify();
}
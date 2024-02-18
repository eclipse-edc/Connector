/*
 *  Copyright (c) 2024 Amadeus
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

package org.eclipse.edc.iam.identitytrust.transform.to;

import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractJwtTransformerTest {

    private AbstractJwtTransformer<Object> transformer;

    @BeforeEach
    void setUp() {
        transformer = new AbstractJwtTransformer<>(Object.class) {
            @Override
            public @Nullable Object transform(@NotNull String s, @NotNull TransformerContext context) {
                return null;
            }
        };
    }


    @Test
    void listOrReturn_null() {
        assertThat(transformer.listOrReturn(null, null)).isEmpty();
    }


    @Test
    void listOrReturn_list() {
        List<Object> obj = List.of("1", "2", "3");

        var result = transformer.listOrReturn(obj, o -> Integer.valueOf(o.toString()));

        assertThat(result).isNotNull().hasSize(3)
                .contains(1, 2, 3);
    }

    @Test
    void listOrReturn_return() {
        Object obj = "2";

        var result = transformer.listOrReturn(obj, o -> Integer.valueOf(o.toString()));

        assertThat(result).isNotNull().hasSize(1)
                .contains(2);
    }

    @Test
    void toInstant_string() {
        Object obj = Instant.now();

        var result = transformer.toInstant(obj.toString());

        assertThat(result).isEqualTo(obj);
    }

    @Test
    void toInstant_map() {
        var now = Instant.now();
        var obj = Map.of("value", now.toString());

        var result = transformer.toInstant(obj);

        assertThat(result).isEqualTo(now);
    }
}
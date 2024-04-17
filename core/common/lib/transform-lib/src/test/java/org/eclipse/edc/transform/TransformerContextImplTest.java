/*
 *  Copyright (c) 2022 - 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.transform;

import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransformerContextImplTest {

    private final TypeTransformerRegistry registry = mock();
    private final TransformerContext context = new TransformerContextImpl(registry);

    @Test
    void shouldReturnTransformedInput() {
        when(registry.transformerFor(anyString(), eq(Integer.class))).thenReturn(new StringIntegerTypeTransformer());

        var result = context.transform("5", Integer.class);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void shouldCollectProblems_whenTransformFails() {
        when(registry.transformerFor(anyString(), eq(Integer.class))).thenReturn(new StringIntegerTypeTransformer());

        var result = context.transform("not an integer", Integer.class);

        assertThat(result).isEqualTo(null);
        assertThat(context.getProblems()).hasSize(1);
    }

    @Test
    void shouldNotTransform_whenInputIsNull() {
        var result = context.transform(null, Integer.class);

        assertThat(result).isNull();
        verifyNoInteractions(registry);
    }

    @Nested
    class Data {
        @Test
        void shouldClearData_whenConsumed() {
            context.setData(Integer.class, "key", "value");

            var data = context.consumeData(Integer.class, "key");

            assertThat(data).isEqualTo("value");
            assertThat(context.consumeData(Integer.class, "key")).isNull();
        }

        @Test
        void shouldReturnNull_whenKeyDoesNotExist() {
            context.setData(Integer.class, "key", "value");

            var data = context.consumeData(Integer.class, "unexistent-key");

            assertThat(data).isNull();
        }

        @Test
        void shouldReturnNull_whenTypeDoesNotExist() {
            var data = context.consumeData(Double.class, "any");

            assertThat(data).isNull();
        }

        @Test
        void shouldPermitMultipleDataPerType() {
            context.setData(Integer.class, "key", "value");
            context.setData(Integer.class, "another-key", "another-value");

            var data = context.consumeData(Integer.class, "key");

            assertThat(data).isEqualTo("value");
        }
    }
}

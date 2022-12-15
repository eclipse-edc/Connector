/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.transform.spi;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TypeTransformerRegistryImplTest {

    private final TypeTransformerRegistryImpl<TypeTransformer<?, ?>> registry = new TypeTransformerRegistryImpl<>();

    @BeforeEach
    void setUp() {
        registry.register(new StringIntegerTypeTransformer());
    }

    @Test
    void shouldTransform_whenInputAndOutputTypesAreHandledByRegisteredTransformer() {
        var result = registry.transform("5", Integer.class);

        assertThat(result).matches(Result::succeeded).extracting(Result::getContent).isEqualTo(5);
    }

    @Test
    void shouldFail_whenTransformerFails() {
        var result = registry.transform("not an integer", Integer.class);

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages).asList().hasSize(1);
    }

    @Test
    void shouldThrowException_whenTransformerIsNotFound() {
        assertThatThrownBy(() -> registry.transform(3, String.class)).isInstanceOf(EdcException.class);
    }

    @Test
    void shouldThrowException_whenInputIsNull() {
        assertThatThrownBy(() -> registry.transform(null, Integer.class)).isInstanceOf(NullPointerException.class);
    }

    private static class StringIntegerTypeTransformer implements TypeTransformer<String, Integer> {
        @Override
        public Class<String> getInputType() {
            return String.class;
        }

        @Override
        public Class<Integer> getOutputType() {
            return Integer.class;
        }

        @Override
        public @Nullable Integer transform(@Nullable String object, @NotNull TransformerContext context) {
            try {
                return Integer.valueOf(object);
            } catch (Exception e) {
                context.reportProblem(format("String %s cannot be transformed to integer: %s", object, e.getMessage()));
                return null;
            }

        }
    }
}

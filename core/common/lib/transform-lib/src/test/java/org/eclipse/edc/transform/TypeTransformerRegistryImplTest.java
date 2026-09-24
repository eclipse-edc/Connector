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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class TypeTransformerRegistryImplTest {

    private final Monitor monitor = mock();
    private final TypeTransformerRegistry registry = new TypeTransformerRegistryImpl(monitor);

    @BeforeEach
    void setUp() {
        registry.register(new StringIntegerTypeTransformer());
    }

    @Nested
    class TransformerFor {

        @Test
        void shouldReturnTheCorrectTransformer() {
            var transformer = registry.transformerFor("a string", Integer.class);

            assertThat(transformer).isInstanceOf(StringIntegerTypeTransformer.class);
        }

        @Test
        void shouldReplaceTransformerForTheSameInputAndOutputTypes() {
            var replacement = new TestTypeTransformer<>(String.class, Integer.class);
            registry.register(replacement);

            assertThat(registry.transformerFor("a string", Integer.class)).isSameAs(replacement);
            verify(monitor).warning(contains("Overriding transformer registered"));
        }

        @Test
        void shouldReturnMostSpecificCompatibleTransformer() {
            var objectTransformer = new TestTypeTransformer<>(Object.class, Long.class);
            var charSequenceTransformer = new TestTypeTransformer<>(CharSequence.class, Long.class);
            registry.register(objectTransformer);
            registry.register(charSequenceTransformer);

            assertThat(registry.transformerFor("a string", Long.class)).isSameAs(charSequenceTransformer);
        }

        @Test
        void shouldThrowExceptionWhenCompatibleTransformersAreAmbiguous() {
            registry.register(new TestTypeTransformer<>(CharSequence.class, Long.class));
            registry.register(new TestTypeTransformer<>(Comparable.class, Long.class));

            assertThatThrownBy(() -> registry.transformerFor("a string", Long.class))
                    .isInstanceOf(EdcException.class)
                    .hasMessageContaining("Ambiguous transformers");
        }

        @Test
        void shouldThrowExceptionWhenTransformerDoesNotExist() {
            var notString = 4L;
            assertThatThrownBy(() -> registry.transformerFor(notString, Integer.class)).isInstanceOf(EdcException.class);
            assertThatThrownBy(() -> registry.transformerFor(String.class, Long.class)).isInstanceOf(EdcException.class);
            assertThatThrownBy(() -> registry.transformerFor(notString, Integer.class)).isInstanceOf(EdcException.class);
            assertThatThrownBy(() -> registry.transformerFor(notString, Float.class)).isInstanceOf(EdcException.class);
        }
    }

    @Nested
    class ForContext {

        private final TypeTransformerRegistry contextRegistry = registry.forContext("context");

        @Test
        void shouldTransformUsingDefaultTransformer() {
            var result = contextRegistry.transform("5", Integer.class);

            assertThat(result).isSucceeded().isEqualTo(5);
        }

        @Test
        void shouldReturnRegistryForSpecificContext() {
            contextRegistry.register(new IntegerStringTypeTransformer());

            assertThat(contextRegistry.transform(5, String.class))
                    .isSucceeded().isEqualTo("5");
            assertThatThrownBy(() -> registry.transform(5, String.class)).isInstanceOf(EdcException.class);
        }

        @Test
        void shouldReturnRegistryForSpecificNestedContext() {
            var nestedContextRegistry = contextRegistry.forContext("nestedContext");
            nestedContextRegistry.register(new IntegerStringTypeTransformer());

            assertThat(nestedContextRegistry.transform(5, String.class))
                    .isSucceeded().isEqualTo("5");
            assertThatThrownBy(() -> contextRegistry.transform(5, String.class)).isInstanceOf(EdcException.class);
            assertThatThrownBy(() -> registry.transform(5, String.class)).isInstanceOf(EdcException.class);
        }

        @Test
        void shouldTransformUsingNestedContext() {
            var nestedContextRegistry = contextRegistry.forContext("nestedContext");
            nestedContextRegistry.register(new IntegerStringTypeTransformer());

            TypeTransformer<Integer, String> typeTransformer = mock();
            contextRegistry.register(typeTransformer);
            registry.register(typeTransformer);
            clearInvocations((Object) typeTransformer);

            assertThat(nestedContextRegistry.transform(5, String.class))
                    .isSucceeded().isEqualTo("5");

            verifyNoInteractions(typeTransformer);
        }

        @Test
        void shouldOverrideParentTransformer() {
            var replacement = new TestTypeTransformer<>(String.class, Integer.class);
            contextRegistry.register(replacement);

            assertThat(contextRegistry.transformerFor("a string", Integer.class)).isSameAs(replacement);
            assertThat(registry.transformerFor("a string", Integer.class)).isInstanceOf(StringIntegerTypeTransformer.class);
        }

    }

    @Nested
    class Transform {
        @Test
        void transform_shouldSucceed_whenInputAndOutputTypesAreHandledByRegisteredTransformer() {
            var result = registry.transform("5", Integer.class);

            assertThat(result).isSucceeded().isEqualTo(5);
        }

        @Test
        void transform_shouldFail_whenTransformerFails() {
            var result = registry.transform("not an integer", Integer.class);

            assertThat(result).isFailed().messages().hasSize(1);
        }

        @Test
        void transform_shouldThrowException_whenTransformerIsNotFound() {
            assertThatThrownBy(() -> registry.transform(3, String.class)).isInstanceOf(EdcException.class);
        }

        @Test
        void transform_shouldThrowException_whenInputIsNull() {
            assertThatThrownBy(() -> registry.transform(null, Integer.class)).isInstanceOf(NullPointerException.class);
        }
    }

}

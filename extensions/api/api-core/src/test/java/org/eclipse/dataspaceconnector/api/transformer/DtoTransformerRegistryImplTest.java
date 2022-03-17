/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.transformer;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class DtoTransformerRegistryImplTest {

    private DtoTransformerRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new DtoTransformerRegistryImpl();
    }

    @Test
    void register() {
        var transformer = new TestTransformer();
        registry.register(transformer);
        assertThat(registry.getTransformers()).containsOnly(transformer);
    }

    @Test
    void register_many() {
        registry.register(new TestTransformer());
        registry.register(new TestTransformer());
        assertThat(registry.getTransformers()).hasSize(2);
    }

    @Test
    void register_same() {
        var t = new TestTransformer();
        registry.register(t);
        registry.register(t);
        assertThat(registry.getTransformers()).hasSize(1).containsOnly(t);
    }

    @Test
    void transform_canHandle_success() {
        registry.register(new TestTransformer());

        var r = registry.transform("foo", String.class);
        assertThat(r.succeeded()).isTrue();
        assertThat(r.getContent()).isEqualTo("foo");
    }

    @Test
    void transform_cannotHandle() {
        registry.register(new TestTransformer());

        assertThatThrownBy(() -> registry.transform("foo", Integer.class)).isInstanceOf(EdcException.class)
                .hasMessageStartingWith("No ApiTransformer registered that can handle foo -> " + Integer.class);
    }

    @Test
    void transform_canHandle_withErrors() {

        var tr = new FailingTransformer();
        registry.register(tr);

        var r = registry.transform("foo", String.class);
        assertThat(r.succeeded()).isFalse();
        assertThat(r.getFailureMessages()).containsExactly("some problem");

    }

    private static class TestTransformer implements DtoTransformer<String, String> {

        @Override
        public Class<String> getInputType() {
            return String.class;
        }

        @Override
        public Class<String> getOutputType() {
            return String.class;
        }

        @Override
        public @Nullable String transform(@Nullable String object, @NotNull TransformerContext context) {
            return object;
        }
    }

    private static class FailingTransformer extends TestTransformer {
        @Override
        public @Nullable String transform(@Nullable String object, @NotNull TransformerContext context) {
            context.reportProblem("some problem");
            return super.transform(object, context);
        }
    }
}
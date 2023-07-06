/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.configuration.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ManagementApiTypeTransformerRegistryImplTest {

    @Test
    void shouldUseRegisteredTransformer() {
        var fallbackRegistry = spy(new TypeTransformerRegistryImpl());
        TypeTransformer<String, JsonObject> fallbackTransformer = spy(new StringJsonObjectTypeTransformer());
        fallbackRegistry.register(fallbackTransformer);
        ManagementApiTypeTransformerRegistry transformerRegistry = new ManagementApiTypeTransformerRegistryImpl(fallbackRegistry);
        TypeTransformer<String, JsonObject> transformer = spy(new StringJsonObjectTypeTransformer());
        transformerRegistry.register(transformer);

        transformerRegistry.transform("{}", JsonObject.class);

        verify(transformer).transform(eq("{}"), any());
        verifyNoInteractions(fallbackTransformer);
    }

    @Test
    void shouldUsePassedRegistryTransformer_whenNoTransformerFound() {
        var fallbackRegistry = new TypeTransformerRegistryImpl();
        TypeTransformer<String, JsonObject> fallbackTransformer = spy(new StringJsonObjectTypeTransformer());
        fallbackRegistry.register(fallbackTransformer);
        ManagementApiTypeTransformerRegistry transformerRegistry = new ManagementApiTypeTransformerRegistryImpl(fallbackRegistry);

        transformerRegistry.transform("{}", JsonObject.class);

        verify(fallbackTransformer).transform(eq("{}"), any());
    }

    private static class StringJsonObjectTypeTransformer implements TypeTransformer<String, JsonObject> {
        @Override
        public Class<String> getInputType() {
            return String.class;
        }

        @Override
        public Class<JsonObject> getOutputType() {
            return JsonObject.class;
        }

        @Override
        public @Nullable JsonObject transform(@NotNull String s, @NotNull TransformerContext context) {
            return Json.createObjectBuilder().build();
        }
    }
}

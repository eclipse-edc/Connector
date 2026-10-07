/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.web.jersey.providers.jsonld;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObjectMapperProviderTest {

    private final TypeManager typeManager = mock();
    private final ObjectMapperProvider provider = new ObjectMapperProvider(typeManager, "test-context");

    @Test
    void getContext_shouldReturnMapperOfTypeContext() {
        var mapper = JsonMapper.builder().build();
        when(typeManager.getMapper("test-context")).thenReturn(mapper);

        assertThat(provider.getContext(Object.class)).isSameAs(mapper);
    }

    @Test
    void getContext_whenMapperIsNoJsonMapper_shouldThrow() {
        when(typeManager.getMapper("test-context")).thenReturn(new ObjectMapper());

        // Jersey would otherwise fall back to its own mapper, which ignores the configuration of the type context
        assertThatThrownBy(() -> provider.getContext(Object.class))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("test-context");
    }
}

/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.transform.transformer.edc.to;

import jakarta.json.JsonValue;
import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JsonValueToGenericTypeTransformerTest {

    private final JsonValueToGenericTypeTransformer transformer = new JsonValueToGenericTypeTransformer(new JacksonTypeManager(), "any");

    @Test
    void shouldConvertBooleanTrue() {
        var transform = transformer.transform(JsonValue.TRUE, mock());

        assertThat(transform).isInstanceOf(Boolean.class).isEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldConvertBooleanFalse() {
        var transform = transformer.transform(JsonValue.FALSE, mock());

        assertThat(transform).isInstanceOf(Boolean.class).isEqualTo(Boolean.FALSE);
    }
}

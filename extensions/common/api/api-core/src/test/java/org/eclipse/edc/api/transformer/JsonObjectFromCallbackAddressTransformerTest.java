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

package org.eclipse.edc.api.transformer;


import jakarta.json.Json;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.AUTH_CODE_ID;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.AUTH_KEY;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;
import static org.mockito.Mockito.mock;

class JsonObjectFromCallbackAddressTransformerTest {

    private JsonObjectFromCallbackAddressTransformer transformer;

    @BeforeEach
    void setup() {
        transformer = new JsonObjectFromCallbackAddressTransformer(Json.createBuilderFactory(Map.of()));
    }

    @Test
    void transform() {
        var callbackAddr = CallbackAddress.Builder.newInstance()
                .uri("http://test.local")
                .events(Set.of("foo", "bar", "baz"))
                .transactional(true)
                .authKey("key")
                .authCodeId("codeId")
                .build();

        var json = transformer.transform(callbackAddr, mock(TransformerContext.class));
        assertThat(json).isNotNull();
        assertThat(json.getJsonString(URI).getString()).isEqualTo("http://test.local");
        assertThat(json.get(IS_TRANSACTIONAL).toString()).isEqualTo("true");
        assertThat(json.getJsonArray(EVENTS)).hasSize(3);
        assertThat(json.getJsonString(AUTH_KEY).getString()).isEqualTo("key");
        assertThat(json.getJsonString(AUTH_CODE_ID).getString()).isEqualTo("codeId");

    }
}

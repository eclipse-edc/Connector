/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.from;


import jakarta.json.Json;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.controlplane.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.eclipse.edc.controlplane.CallbackAddress.AUTH_CODE_ID;
import static org.eclipse.edc.controlplane.CallbackAddress.AUTH_KEY;
import static org.eclipse.edc.controlplane.CallbackAddress.EVENTS;
import static org.eclipse.edc.controlplane.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.controlplane.CallbackAddress.URI;
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
        Assertions.assertThat(json).isNotNull();
        Assertions.assertThat(json.getJsonString(URI).getString()).isEqualTo("http://test.local");
        Assertions.assertThat(json.get(IS_TRANSACTIONAL).toString()).isEqualTo("true");
        Assertions.assertThat(json.getJsonArray(EVENTS)).hasSize(3);
        Assertions.assertThat(json.getJsonString(AUTH_KEY).getString()).isEqualTo("key");
        Assertions.assertThat(json.getJsonString(AUTH_CODE_ID).getString()).isEqualTo("codeId");

    }
}

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

package org.eclipse.edc.connector.controlplane.transform.to;

import jakarta.json.Json;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.controlplane.CallbackAddress.AUTH_CODE_ID;
import static org.eclipse.edc.controlplane.CallbackAddress.AUTH_KEY;
import static org.eclipse.edc.controlplane.CallbackAddress.EVENTS;
import static org.eclipse.edc.controlplane.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.controlplane.CallbackAddress.URI;
import static org.eclipse.edc.jsonld.test.TestJsonLd.expand;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToCallbackAddressTransformerTest {

    private final TypeManager typeManager = mock();
    private JsonObjectToCallbackAddressTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToCallbackAddressTransformer();
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var jobj = Json.createObjectBuilder()
                .add(IS_TRANSACTIONAL, true)
                .add(URI, "http://test.local/")
                .add(EVENTS, Json.createArrayBuilder()
                        .add("foo")
                        .add("bar")
                        .add("baz")
                        .build())
                .add(AUTH_CODE_ID, "code")
                .add(AUTH_KEY, "key")
                .build();

        var contextMock = mock(TransformerContext.class);
        var genericTransformer = new JsonValueToGenericTypeTransformer(typeManager, "test");
        when(contextMock.transform(any(), eq(String.class))).thenAnswer(a -> genericTransformer.transform(a.getArgument(0), contextMock));

        var cba = transformer.transform(expand(jobj), contextMock);

        Assertions.assertThat(cba).isNotNull();
        Assertions.assertThat(cba.getEvents()).containsExactlyInAnyOrder("foo", "bar", "baz");
        Assertions.assertThat(cba.getUri()).isEqualTo("http://test.local/");
        Assertions.assertThat(cba.isTransactional()).isTrue();
        Assertions.assertThat(cba.getAuthKey()).isEqualTo("key");
        Assertions.assertThat(cba.getAuthCodeId()).isEqualTo("code");
    }
}

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

package org.eclipse.edc.iam.identitytrust.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.identitytrust.transform.TestObject;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identitytrust.model.CredentialSubject.CREDENTIAL_SUBJECT_ID_PROPERTY;
import static org.mockito.Mockito.mock;

class JsonObjectFromCredentialSubjectTransformerTest {
    private final TransformerContext context = mock();
    private JsonObjectFromCredentialSubjectTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromCredentialSubjectTransformer(Json.createBuilderFactory(Map.of()), JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform_simpleClaims() {
        var subj = CredentialSubject.Builder.newInstance()
                .id("https://there.is.my/thing")
                .claims(Map.of("foo", "bar"))
                .build();
        var json = transformer.transform(subj, context);

        assertThat(json).isNotNull();
        assertThat(json.getString(CREDENTIAL_SUBJECT_ID_PROPERTY)).isEqualTo("https://there.is.my/thing");
        assertThat(json.getString("foo")).isEqualTo("bar");
        assertThat(json.values()).hasSize(2);
    }

    @Test
    void transform_complexClaims() {
        var subj = CredentialSubject.Builder.newInstance()
                .id("https://there.is.my/thing")
                .claims(Map.of("complexFoo", new TestObject("bar", Map.of("baz", "goo", "answer", 42))))
                .build();

        var json = transformer.transform(subj, context);

        assertThat(json).isNotNull();
        assertThat(json.getString(CREDENTIAL_SUBJECT_ID_PROPERTY)).isEqualTo("https://there.is.my/thing");
        assertThat(json.getJsonObject("complexFoo"))
                .satisfies(map -> {
                    assertThat(map.size()).isEqualTo(2);
                    assertThat(((JsonObject) map).getString("foo")).isEqualTo("bar");
                    assertThat(map.get("baz")).isInstanceOf(Map.class);
                    var innerMap = map.get("baz");
                    assertThat(innerMap.asJsonObject().getString("baz")).isEqualTo("goo");
                    assertThat(innerMap.asJsonObject().getJsonNumber("answer").intValue()).isEqualTo(42);
                });
        assertThat(json.values()).hasSize(2);
    }
}
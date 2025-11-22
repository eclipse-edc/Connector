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

package org.eclipse.edc.iam.decentralizedclaims.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.transform.TestObject;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.TEST_NAMESPACE;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject.CREDENTIAL_SUBJECT_ID_PROPERTY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToCredentialSubjectTransformerTest {

    private final TransformerContext context = mock();
    private final JsonLd jsonLdService = new TitaniumJsonLd(mock());
    private JsonObjectToCredentialSubjectTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToCredentialSubjectTransformer();
    }

    @Test
    void transform_simpleClaims_withoutId() {
        var json = Json.createObjectBuilder()
                .add(TEST_NAMESPACE + "simpleclaim1", "value1")
                .add(TEST_NAMESPACE + "simpleclaim2", "value2")
                .build();
        when(context.transform(isA(JsonObject.class), eq(Object.class))).thenAnswer(a -> ((JsonObject) a.getArgument(0)).getString(VALUE));
        var subj = transformer.transform(jsonLdService.expand(json).getContent(), context);

        assertThat(subj).isNotNull();
        assertThat(subj.getId()).isNull();
        assertThat(subj.getClaims())
                .containsEntry(TEST_NAMESPACE + "simpleclaim1", "value1")
                .containsEntry(TEST_NAMESPACE + "simpleclaim2", "value2");
    }

    @Test
    void transform_simpleClaims() {
        var json = Json.createObjectBuilder()
                .add(CREDENTIAL_SUBJECT_ID_PROPERTY, "test-id")
                .add(TEST_NAMESPACE + "simpleclaim1", "value1")
                .add(TEST_NAMESPACE + "simpleclaim2", "value2")
                .build();
        when(context.transform(isA(JsonObject.class), eq(Object.class))).thenAnswer(a -> ((JsonObject) a.getArgument(0)).getString(VALUE));
        var subj = transformer.transform(jsonLdService.expand(json).getContent(), context);

        assertThat(subj).isNotNull();
        assertThat(subj.getId()).isEqualTo("test-id");
        assertThat(subj.getClaims())
                .containsEntry(TEST_NAMESPACE + "simpleclaim1", "value1")
                .containsEntry(TEST_NAMESPACE + "simpleclaim2", "value2");
    }

    @Test
    void transform_complexClaims() {
        var json = Json.createObjectBuilder()
                .add(CREDENTIAL_SUBJECT_ID_PROPERTY, "test-id")
                .add(TEST_NAMESPACE + "complexClaim", Json.createObjectBuilder()
                        .add(TEST_NAMESPACE + "foo", "bar")
                        .add(TEST_NAMESPACE + "baz", Json.createObjectBuilder().add(TEST_NAMESPACE + "goo", 42)))
                .build();

        when(context.transform(isA(JsonObject.class), eq(Object.class))).thenReturn(new TestObject("bar", Map.of("goo", 42)));
        var subj = transformer.transform(jsonLdService.expand(json).getContent(), context);

        assertThat(subj).isNotNull();
        assertThat(subj.getId()).isEqualTo("test-id");
        assertThat(subj.getClaims()).isNotNull().isNotEmpty()
                .allSatisfy((s, o) -> {
                    assertThat(s).isEqualTo(TEST_NAMESPACE + "complexClaim");
                    assertThat(o).isInstanceOf(TestObject.class);
                    var to = (TestObject) o;
                    assertThat(to.foo()).isEqualTo("bar");
                    assertThat(to.baz()).containsEntry("goo", 42);
                });
    }
}

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
import jakarta.json.JsonValue;
import org.eclipse.edc.iam.decentralizedclaims.transform.TestObject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.transform.TestData.TEST_NAMESPACE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToCredentialStatusTransformerTest {

    private final TransformerContext context = mock();
    private final JsonLd jsonLdService = new TitaniumJsonLd(mock());
    private JsonObjectToCredentialStatusTransformer transformer;

    @BeforeEach
    void setup() {
        transformer = new JsonObjectToCredentialStatusTransformer();
    }

    @Test
    void transform_noProperties() {
        var jsonObj = Json.createObjectBuilder()
                .add(CredentialStatus.CREDENTIAL_STATUS_ID_PROPERTY, "test-id")
                .add(CredentialStatus.CREDENTIAL_STATUS_TYPE_PROPERTY, "SomeTestCredential")
                .build();
        var status = transformer.transform(jsonLdService.expand(jsonObj).getContent(), context);
        assertThat(status).isNotNull();
        assertThat(status.id()).isEqualTo("test-id");
        assertThat(status.type()).isEqualTo("SomeTestCredential");
        assertThat(status.additionalProperties()).isNotNull().isEmpty();
    }

    @Test
    void transform_withProperties() {

        var jsonObj = Json.createObjectBuilder()
                .add(CredentialStatus.CREDENTIAL_STATUS_ID_PROPERTY, "test-id")
                .add(CredentialStatus.CREDENTIAL_STATUS_TYPE_PROPERTY, "SomeTestCredential")
                .add(TEST_NAMESPACE + "someprop", "someval")
                .add(TEST_NAMESPACE + "someOtherProp", "someOtherVal")
                .build();
        when(context.transform(isA(JsonValue.class), eq(Object.class))).thenAnswer(a -> ((JsonObject) a.getArgument(0)).getString(VALUE));

        var status = transformer.transform(jsonLdService.expand(jsonObj).getContent(), context);
        assertThat(status).isNotNull();
        assertThat(status.id()).isEqualTo("test-id");
        assertThat(status.type()).isEqualTo("SomeTestCredential");
        assertThat(status.additionalProperties()).isNotNull()
                .containsEntry(TEST_NAMESPACE + "someprop", "someval")
                .containsEntry(TEST_NAMESPACE + "someOtherProp", "someOtherVal");
    }

    @Test
    void transform_withComplexProperties() {

        var jsonObj = Json.createObjectBuilder()
                .add(CredentialStatus.CREDENTIAL_STATUS_ID_PROPERTY, "test-id")
                .add(CredentialStatus.CREDENTIAL_STATUS_TYPE_PROPERTY, "SomeTestCredential")
                .add(TEST_NAMESPACE + "someComplexProp", Json.createObjectBuilder()
                        .add(TEST_NAMESPACE + "foo", "bar")
                        .add(TEST_NAMESPACE + "baz", Json.createObjectBuilder().add(TEST_NAMESPACE + "goo", 42)))
                .build();
        when(context.transform(isA(JsonObject.class), eq(Object.class))).thenReturn(new TestObject("bar", Map.of("goo", 42)));

        var status = transformer.transform(jsonLdService.expand(jsonObj).getContent(), context);
        assertThat(status).isNotNull();
        assertThat(status.id()).isEqualTo("test-id");
        assertThat(status.type()).isEqualTo("SomeTestCredential");
        assertThat(status.additionalProperties()).isNotNull().isNotEmpty()
                .allSatisfy((s, o) -> {
                    assertThat(s).isEqualTo(TEST_NAMESPACE + "someComplexProp");
                    assertThat(o).isInstanceOf(TestObject.class);
                    var to = (TestObject) o;
                    assertThat(to.foo()).isEqualTo("bar");
                    assertThat(to.baz()).containsEntry("goo", 42);
                });
    }

}
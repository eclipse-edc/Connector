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
import jakarta.json.JsonString;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToIssuerTransformerTest {

    private final TransformerContext context = mock();
    private JsonObjectToIssuerTransformer transformer;

    @BeforeEach
    void setup() {
        transformer = new JsonObjectToIssuerTransformer();
    }

    @DisplayName("Asserts the correct parsing of an issuer that is a URL")
    @Test
    void transform_issuerIsUrl() {
        var url = "https://some-test.issuer.org";
        var jobj = Json.createObjectBuilder().add("@id", url).build();
        var issuer = transformer.transform(jobj, context);
        assertThat(issuer).isNotNull();
        assertThat(issuer.id()).isEqualTo(url);
        assertThat(issuer.additionalProperties()).isEmpty();
    }

    @DisplayName("Asserts the correct parsing of an issuer that is an object")
    @Test
    void transform_issuerIsObject() {
        var url = "https://some-test.issuer.org";
        var issuerObj = Json.createObjectBuilder()
                .add("@id", url)
                .add("name", "test-name")
                .add("desc", "test-desc")
                .build();
        when(context.transform(any(), any())).thenAnswer(a -> ((JsonString) a.getArgument(0)).getString());

        var issuer = transformer.transform(issuerObj, context);
        assertThat(issuer).isNotNull();
        assertThat(issuer.id()).isEqualTo(url);
        assertThat(issuer.additionalProperties()).containsEntry("name", "test-name")
                .containsEntry("desc", "test-desc");
    }

    @DisplayName("Asserts the correct parsing of an issuer that is a number (invalid)")
    @Test
    void transform_issuerIsUnexpectedType() {
        var jobj = Json.createObjectBuilder().add("@id", 42).build();
        when(context.transform(any(), any())).thenAnswer(a -> ((JsonString) a.getArgument(0)).getString());

        assertThatThrownBy(() -> transformer.transform(jobj, context)).isInstanceOf(NullPointerException.class);
    }

}
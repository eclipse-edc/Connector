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
import org.eclipse.edc.identitytrust.model.CredentialStatus;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identitytrust.model.CredentialStatus.CREDENTIAL_STATUS_ID_PROPERTY;
import static org.eclipse.edc.identitytrust.model.CredentialStatus.CREDENTIAL_STATUS_TYPE_PROPERTY;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.mockito.Mockito.mock;

class JsonObjectFromCredentialStatusTransformerTest {

    private JsonObjectFromCredentialStatusTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromCredentialStatusTransformer(Json.createBuilderFactory(Map.of()), createObjectMapper());
    }

    @Test
    void transform_withAdditionalProperties() {
        // example taken from https://w3c.github.io/vc-data-model/#status
        var id = "https://university.example/credentials/status/3#94567";
        var type = "StatusList2021Entry";
        var status = new CredentialStatus(id, type,
                Map.of("statusPurpose", "revocation", "statusListIndex", "94567", "statusListCredential", "https://university.example/credentials/status/3"));

        var context = mock(TransformerContext.class);
        var jsonLd = transformer.transform(status, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.values()).hasSize(5);
        assertThat(jsonLd.getString(CREDENTIAL_STATUS_ID_PROPERTY)).isEqualTo(id);
        assertThat(jsonLd.getString(CREDENTIAL_STATUS_TYPE_PROPERTY)).isEqualTo(type);
        assertThat(jsonLd.getString("statusPurpose")).isEqualTo("revocation");
        assertThat(jsonLd.getString("statusListIndex")).isEqualTo("94567");
        assertThat(jsonLd.getString("statusListCredential")).isEqualTo("https://university.example/credentials/status/3");
    }

    @Test
    void transform_noAdditionalProperties() {
        // example taken from https://w3c.github.io/vc-data-model/#status
        var id = "https://university.example/credentials/status/3#94567";
        var type = "StatusList2021Entry";
        var status = new CredentialStatus(id, type, Map.of());

        var context = mock(TransformerContext.class);
        var jsonLd = transformer.transform(status, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.values()).hasSize(2);
        assertThat(jsonLd.getString(CREDENTIAL_STATUS_ID_PROPERTY)).isEqualTo(id);
        assertThat(jsonLd.getString(CREDENTIAL_STATUS_TYPE_PROPERTY)).isEqualTo(type);
    }
}
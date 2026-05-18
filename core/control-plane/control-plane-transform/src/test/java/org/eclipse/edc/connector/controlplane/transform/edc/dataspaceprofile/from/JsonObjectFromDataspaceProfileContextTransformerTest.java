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

package org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_NAME_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_TYPE_IRI;
import static org.mockito.Mockito.mock;

class JsonObjectFromDataspaceProfileContextTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private JsonObjectFromDataspaceProfileContextTransformer transformer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDataspaceProfileContextTransformer(jsonFactory);
        context = mock(TransformerContext.class);
    }

    @Test
    void transform_shouldConvertAllFields() {
        var profile = new DataspaceProfileContext(
                "profile-name",
                new ProtocolVersion("1.0", "/2024/1", "HTTPS"),
                () -> "https://example.com/webhook",
                mock(),
                new JsonLdNamespace("https://example.com/ns/"),
                List.of("https://example.com/ctx/v1", "https://example.com/ctx/v2"));

        var result = transformer.transform(profile, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(TYPE)).isEqualTo(DATASPACE_PROFILE_CONTEXT_TYPE_IRI);
        assertThat(result.getString(DATASPACE_PROFILE_CONTEXT_NAME_IRI)).isEqualTo("profile-name");

        var protocolVersion = result.getJsonObject(DATASPACE_PROFILE_CONTEXT_PROTOCOL_IRI);
        assertThat(protocolVersion).isNotNull();
        assertThat(protocolVersion.getString(DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_IRI)).isEqualTo("1.0");
        assertThat(protocolVersion.getString(DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_IRI)).isEqualTo("/2024/1");
        assertThat(protocolVersion.getString(DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_IRI)).isEqualTo("HTTPS");
        assertThat(protocolVersion.getString(DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_IRI)).isEqualTo("https://example.com/ns/");


        var contexts = result.getJsonArray(DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_IRI);
        assertThat(contexts).hasSize(2);
        assertThat(contexts.getString(0)).isEqualTo("https://example.com/ctx/v1");
        assertThat(contexts.getString(1)).isEqualTo("https://example.com/ctx/v2");
    }


    @Test
    void transform_shouldHandleEmptyContextsList() {
        var profile = new DataspaceProfileContext(
                "profile-name",
                new ProtocolVersion("1.0", "/", "HTTPS"),
                () -> "https://example.com/webhook",
                mock(),
                new JsonLdNamespace("https://example.com/ns/"),
                List.of());

        var result = transformer.transform(profile, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonArray(DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_IRI)).isNotNull().isEmpty();
    }
}

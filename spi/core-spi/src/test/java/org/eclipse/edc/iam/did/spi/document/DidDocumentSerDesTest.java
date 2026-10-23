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

package org.eclipse.edc.iam.did.spi.document;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DidDocumentSerDesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void verifyDeserialization() throws JacksonException {
        var json = """
                {
                  "@context": ["https://www.w3.org/ns/did/v1"],
                  "id": "did:web:example.com",
                  "authentication": ["did:web:example.com#presentation-key"],
                  "capabilityInvocation": ["did:web:example.com#token-key"],
                  "assertionMethod": ["did:web:example.com#credential-key"]
                }
                """;

        var document = objectMapper.readValue(json, DidDocument.class);

        assertThat(document.getAuthentication()).containsExactly("did:web:example.com#presentation-key");
        assertThat(document.getCapabilityInvocation()).containsExactly("did:web:example.com#token-key");
        assertThat(document.getAssertionMethod()).containsExactly("did:web:example.com#credential-key");
    }

    @SuppressWarnings("unchecked")
    @Test
    void verifyDeserialization_withEmbeddedVerificationMethods() throws JacksonException {
        var json = """
                {
                  "id": "did:web:example.com",
                  "authentication": [{"id": "did:web:example.com#presentation-key", "type": "JsonWebKey2020"}],
                  "capabilityInvocation": ["did:web:example.com#token-key"],
                  "assertionMethod": [{"id": "did:web:example.com#credential-key", "type": "JsonWebKey2020"}]
                }
                """;

        var document = objectMapper.readValue(json, DidDocument.class);

        assertThat(document.getAuthentication()).containsExactly("did:web:example.com#presentation-key");
        assertThat(document.getCapabilityInvocation()).containsExactly("did:web:example.com#token-key");
        assertThat(document.getAssertionMethod()).containsExactly("did:web:example.com#credential-key");
    }

    @Test
    void verifyDeserialization_embeddedVerificationMethodWithoutId_shouldFail() {
        var json = """
                {
                  "id": "did:web:example.com",
                  "assertionMethod": [{"type": "JsonWebKey2020"}]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, DidDocument.class)).isInstanceOf(JacksonException.class);
    }

    @Test
    void verifySerialization() throws JacksonException {
        var document = DidDocument.Builder.newInstance()
                .id("did:web:example.com")
                .authentication(List.of("did:web:example.com#presentation-key"))
                .capabilityInvocation(List.of("did:web:example.com#token-key"))
                .assertionMethod(List.of("did:web:example.com#credential-key"))
                .build();

        var json = objectMapper.readValue(objectMapper.writeValueAsString(document), Map.class);

        assertThat(json)
                .containsEntry("authentication", List.of("did:web:example.com#presentation-key"))
                .containsEntry("capabilityInvocation", List.of("did:web:example.com#token-key"))
                .containsEntry("assertionMethod", List.of("did:web:example.com#credential-key"));
    }
}

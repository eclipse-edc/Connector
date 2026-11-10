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

package org.eclipse.edc.crawler.spi;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TargetNodeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void counterPartyId_shouldFallbackToId_whenNotSet() {
        var node = new TargetNode("name", "did:web:node", "http://example.com", "participant", List.of("protocol"));

        assertThat(node.counterPartyId()).isEqualTo("did:web:node");
    }

    @Test
    void counterPartyId_shouldFallbackToId_whenExplicitlyNull() {
        var node = new TargetNode("name", "did:web:node", null, "http://example.com", "participant", List.of("protocol"));

        assertThat(node.counterPartyId()).isEqualTo("did:web:node");
    }

    @Test
    void counterPartyId_shouldBeKept_whenSet() {
        var node = new TargetNode("name", "catalog-1", "did:web:identity", "http://example.com", "participant", List.of("protocol"));

        assertThat(node.id()).isEqualTo("catalog-1");
        assertThat(node.counterPartyId()).isEqualTo("did:web:identity");
    }

    @Test
    void shouldSerializeAndDeserialize() {
        var node = new TargetNode("name", "catalog-1", "did:web:identity", "http://example.com", "participant", List.of("protocol"));

        var json = mapper.writeValueAsString(node);
        var deserialized = mapper.readValue(json, TargetNode.class);

        assertThat(deserialized).isEqualTo(node);
    }

    @Test
    void shouldDeserialize_whenCounterPartyIdMissing() {
        var json = """
                {
                    "name": "name",
                    "id": "did:web:node",
                    "url": "http://example.com",
                    "participantContextId": "participant",
                    "supportedProtocols": ["protocol"]
                }
                """;

        var node = mapper.readValue(json, TargetNode.class);

        assertThat(node.counterPartyId()).isEqualTo("did:web:node");
    }
}

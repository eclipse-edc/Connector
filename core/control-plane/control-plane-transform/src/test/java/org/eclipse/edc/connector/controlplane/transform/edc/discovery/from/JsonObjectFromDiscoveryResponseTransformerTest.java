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

package org.eclipse.edc.connector.controlplane.transform.edc.discovery.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_BINDING_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_COUNTER_PARTY_PATH_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_PROFILE_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_TYPE_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_VERSION_IRI;
import static org.mockito.Mockito.mock;

class JsonObjectFromDiscoveryResponseTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectFromDiscoveryResponseTransformer transformer = new JsonObjectFromDiscoveryResponseTransformer(Json.createBuilderFactory(Map.of()));

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(DiscoveryResponse.class);
        assertThat(transformer.getOutputType()).isEqualTo(JsonObject.class);
    }

    @Test
    void transform() {
        var match = new DiscoveryResponse("profile-1", "version", "/remote", "http");

        var result = transformer.transform(match, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(TYPE)).isEqualTo(DISCOVERY_RESPONSE_TYPE_IRI);
        assertThat(result.getString(DISCOVERY_RESPONSE_PROFILE_IRI)).isEqualTo("profile-1");
        assertThat(result.getString(DISCOVERY_RESPONSE_COUNTER_PARTY_PATH_IRI)).isEqualTo("/remote");
        assertThat(result.getString(DISCOVERY_RESPONSE_VERSION_IRI)).isEqualTo("version");
        assertThat(result.getString(DISCOVERY_RESPONSE_BINDING_IRI)).isEqualTo("http");
    }
}

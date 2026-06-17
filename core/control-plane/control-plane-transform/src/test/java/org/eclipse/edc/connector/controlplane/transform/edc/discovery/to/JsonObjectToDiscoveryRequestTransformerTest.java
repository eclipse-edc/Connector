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

package org.eclipse.edc.connector.controlplane.transform.edc.discovery.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.test.TestJsonLd.expand;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest.DISCOVERY_REQUEST_COUNTER_PARTY_ADDRESS_TERM;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest.DISCOVERY_REQUEST_COUNTER_PARTY_ID_TERM;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest.DISCOVERY_REQUEST_TYPE_TERM;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

class JsonObjectToDiscoveryRequestTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToDiscoveryRequestTransformer transformer = new JsonObjectToDiscoveryRequestTransformer();

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(DiscoveryRequest.class);
    }

    @Test
    void transform_address() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add(TYPE, DISCOVERY_REQUEST_TYPE_TERM)
                .add(DISCOVERY_REQUEST_COUNTER_PARTY_ADDRESS_TERM, "https://example.org")
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNotNull();
        assertThat(result.counterPartyAddress()).isEqualTo("https://example.org");
        assertThat(result.counterPartyId()).isNull();
    }

    @Test
    void transform_id() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add(TYPE, DISCOVERY_REQUEST_TYPE_TERM)
                .add(DISCOVERY_REQUEST_COUNTER_PARTY_ID_TERM, "did:web:example")
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result.counterPartyId()).isEqualTo("did:web:example");
        assertThat(result.counterPartyAddress()).isNull();
    }

    @Test
    void transform_emptyBody() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add(TYPE, DISCOVERY_REQUEST_TYPE_TERM)
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result.counterPartyId()).isNull();
        assertThat(result.counterPartyAddress()).isNull();
    }

}

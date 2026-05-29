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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.COUNTER_PARTY_DATASERVICE_ENDPOINT_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.COUNTER_PARTY_PATH_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_BINDING_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_COUNTER_PARTY_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_PROFILE_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_TYPE_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse.DISCOVERY_RESPONSE_VERSION_IRI;

public class JsonObjectFromDiscoveryResponseTransformer extends AbstractJsonLdTransformer<DiscoveryResponse, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDiscoveryResponseTransformer(JsonBuilderFactory jsonFactory) {
        super(DiscoveryResponse.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DiscoveryResponse match, @NotNull TransformerContext context) {
        var counterParty = jsonFactory.createObjectBuilder()
                .add(COUNTER_PARTY_PATH_IRI, match.counterParty().path())
                .add(COUNTER_PARTY_DATASERVICE_ENDPOINT_IRI, match.counterParty().dataServiceEndpoint())
                .build();

        return jsonFactory.createObjectBuilder()
                .add(TYPE, DISCOVERY_RESPONSE_TYPE_IRI)
                .add(DISCOVERY_RESPONSE_PROFILE_IRI, match.profile())
                .add(DISCOVERY_RESPONSE_VERSION_IRI, match.version())
                .add(DISCOVERY_RESPONSE_COUNTER_PARTY_IRI, counterParty)
                .add(DISCOVERY_RESPONSE_BINDING_IRI, match.binding())
                .build();
    }
}

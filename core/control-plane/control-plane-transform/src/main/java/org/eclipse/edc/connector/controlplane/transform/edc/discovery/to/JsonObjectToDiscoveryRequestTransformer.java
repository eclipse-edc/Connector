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

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest.DISCOVERY_REQUEST_COUNTER_PARTY_ADDRESS_IRI;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest.DISCOVERY_REQUEST_COUNTER_PARTY_ID_IRI;

public class JsonObjectToDiscoveryRequestTransformer extends AbstractJsonLdTransformer<JsonObject, DiscoveryRequest> {

    public JsonObjectToDiscoveryRequestTransformer() {
        super(JsonObject.class, DiscoveryRequest.class);
    }

    @Override
    public @Nullable DiscoveryRequest transform(@NotNull JsonObject request, @NotNull TransformerContext context) {
        var counterPartyId = transformString(request.get(DISCOVERY_REQUEST_COUNTER_PARTY_ID_IRI), context);
        var counterPartyAddress = transformString(request.get(DISCOVERY_REQUEST_COUNTER_PARTY_ADDRESS_IRI), context);
        return new DiscoveryRequest(counterPartyId, counterPartyAddress);
    }
}

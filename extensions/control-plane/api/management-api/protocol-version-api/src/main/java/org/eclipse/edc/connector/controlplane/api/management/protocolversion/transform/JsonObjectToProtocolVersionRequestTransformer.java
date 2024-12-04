/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.protocolversion.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest.Builder;
import static org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest.PROTOCOL_VERSION_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest.PROTOCOL_VERSION_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest.PROTOCOL_VERSION_REQUEST_PROTOCOL;

public class JsonObjectToProtocolVersionRequestTransformer extends AbstractJsonLdTransformer<JsonObject, ProtocolVersionRequest> {

    public JsonObjectToProtocolVersionRequestTransformer() {
        super(JsonObject.class, ProtocolVersionRequest.class);
    }

    @Override
    public @Nullable ProtocolVersionRequest transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var counterPartyAddress = transformString(object.get(PROTOCOL_VERSION_REQUEST_COUNTER_PARTY_ADDRESS), context);
        var counterPartyId = transformString(object.get(PROTOCOL_VERSION_REQUEST_COUNTER_PARTY_ID), context);
        var protocol = transformString(object.get(PROTOCOL_VERSION_REQUEST_PROTOCOL), context);

        var builder = Builder.newInstance()
                .protocol(protocol)
                .counterPartyAddress(counterPartyAddress)
                .counterPartyId(counterPartyId);
        
        return builder.build();
    }

}

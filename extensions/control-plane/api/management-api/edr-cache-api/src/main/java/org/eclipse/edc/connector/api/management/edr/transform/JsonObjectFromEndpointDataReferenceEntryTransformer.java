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

package org.eclipse.edc.connector.api.management.edr.transform;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CONTRACT_NEGOTIATION_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CREATED_AT;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromEndpointDataReferenceEntryTransformer extends AbstractJsonLdTransformer<EndpointDataReferenceEntry, JsonObject> {
    private final JsonBuilderFactory jsonFactory;


    public JsonObjectFromEndpointDataReferenceEntryTransformer(JsonBuilderFactory jsonFactory) {
        super(EndpointDataReferenceEntry.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull EndpointDataReferenceEntry entry, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(ID, entry.getId())
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(EDR_ENTRY_PROVIDER_ID, entry.getProviderId())
                .add(EDR_ENTRY_ASSET_ID, entry.getAssetId())
                .add(EDR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, entry.getTransferProcessId())
                .add(EDR_ENTRY_CREATED_AT, entry.getCreatedAt())
                .add(EDR_ENTRY_CONTRACT_NEGOTIATION_ID, entry.getContractNegotiationId())
                .build();
    }
}

/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.JsonLdKeywords;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_FORMAT;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_CALLBACKADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_CONTRACTAGREEMENT_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_DATAADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_TRANSFERPROCESS_REQUEST_TYPE;


public class JsonObjectFromTransferRequestMessageTransformer extends AbstractJsonLdTransformer<TransferRequestMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferRequestMessageTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(TransferRequestMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }


    @Override
    public @Nullable JsonObject transform(@NotNull TransferRequestMessage transferRequestMessage, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(JsonLdKeywords.ID, String.valueOf(UUID.randomUUID()));
        builder.add(JsonLdKeywords.TYPE, DSPACE_TRANSFERPROCESS_REQUEST_TYPE);

        builder.add(DSPACE_CONTRACTAGREEMENT_TYPE, transferRequestMessage.getContractId());
        builder.add(DCT_FORMAT, transferRequestMessage.getDataDestination().getType());
        builder.add(DSPACE_DATAADDRESS_TYPE, transformDataAddress(transferRequestMessage.getDataDestination(), context)); //TODO move to separate transformer
        builder.add(DSPACE_CALLBACKADDRESS_TYPE, transferRequestMessage.getCallbackAddress());
        builder.add(DSPACE_PROCESSID_TYPE, transferRequestMessage.getProcessId());

        return builder.build();
    }

    private @Nullable JsonObject transformDataAddress(DataAddress address, TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();
    
        if (address == null) {
            return builder.build();
        }
        
        builder.add("type", address.getType());
        
        if (address.getKeyName() != null) {
            builder.add("keyName", address.getKeyName());
        }

        address.getProperties().forEach((k, v) -> builder.add(k, Json.createValue(v)));

        return builder.build();
    }
}

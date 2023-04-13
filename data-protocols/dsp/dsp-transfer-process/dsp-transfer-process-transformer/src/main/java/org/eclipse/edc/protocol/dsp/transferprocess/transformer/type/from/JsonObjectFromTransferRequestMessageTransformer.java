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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.JsonLdKeywords;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;


public class JsonObjectFromTransferRequestMessageTransformer extends AbstractJsonLdTransformer<TransferRequestMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    private final ObjectMapper mapper;

    public JsonObjectFromTransferRequestMessageTransformer(JsonBuilderFactory jsonBuilderFactory, ObjectMapper mapper) {
        super(TransferRequestMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
        this.mapper = mapper;
    }


    @Override
    public @Nullable JsonObject transform(@Nullable TransferRequestMessage transferRequestMessage, @NotNull TransformerContext context) {
        if (transferRequestMessage == null) {
            return null;
        }

        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(JsonLdKeywords.ID, String.valueOf(UUID.randomUUID()));
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "TransferRequestMessage");

        builder.add(DSPACE_SCHEMA + "agreementId", transferRequestMessage.getContractId());
        builder.add(DCT_SCHEMA + "format", "dspace:AmazonS3+Push"); //TODO fix value
        builder.add(DSPACE_SCHEMA + "dataAddress", transformDataAddress(transferRequestMessage.getDataDestination(), context));

        return builder.build();
    }

    //TODO Improve Code and check if properties are shown correct

    private @Nullable JsonObject transformDataAddress(DataAddress address, TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();

        if (address == null) {
            return builder.build();
        }
        if (address.getKeyName() != null) {
            builder.add("keyName", address.getKeyName());
        }

        if (address.getType() != null) {
            builder.add("type", address.getType());
        }

        try {
            var properties = mapper.writeValueAsString(address.getProperties());
            builder.add("properties", properties);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return builder.build();
    }
}

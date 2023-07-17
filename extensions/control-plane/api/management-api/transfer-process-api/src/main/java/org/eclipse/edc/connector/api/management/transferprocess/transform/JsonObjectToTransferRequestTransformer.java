/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.transferprocess.transform;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_ASSET_ID;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_CONNECTOR_ID;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_CONTRACT_ID;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_DATA_DESTINATION;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_PROPERTIES;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_PROTOCOL;

public class JsonObjectToTransferRequestTransformer extends AbstractJsonLdTransformer<JsonObject, TransferRequest> {

    public JsonObjectToTransferRequestTransformer() {
        super(JsonObject.class, TransferRequest.class);
    }

    @Override
    public @Nullable TransferRequest transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = TransferRequest.Builder.newInstance();

        builder.id(nodeId(input));
        visitProperties(input, k -> switch (k) {
            case TRANSFER_REQUEST_CONNECTOR_ADDRESS -> v -> builder.connectorAddress(transformString(v, context));
            case TRANSFER_REQUEST_CONTRACT_ID -> (v) -> builder.contractId(transformString(v, context));
            case TRANSFER_REQUEST_DATA_DESTINATION ->
                    v -> builder.dataDestination(transformObject(v, DataAddress.class, context));
            case TRANSFER_REQUEST_PROPERTIES -> (v) -> transformProperties(v, builder::properties, context);
            case TRANSFER_REQUEST_CALLBACK_ADDRESSES -> (v) -> {
                var addresses = new ArrayList<CallbackAddress>();
                transformArrayOrObject(v, CallbackAddress.class, addresses::add, context);
                builder.callbackAddresses(addresses);
            };
            case TRANSFER_REQUEST_PRIVATE_PROPERTIES ->
                    (v) -> transformProperties(v, builder::privateProperties, context);
            case TRANSFER_REQUEST_PROTOCOL -> (v) -> builder.protocol(transformString(v, context));
            case TRANSFER_REQUEST_CONNECTOR_ID -> (v) -> builder.connectorId(transformString(v, context));
            case TRANSFER_REQUEST_ASSET_ID -> (v) -> builder.assetId(transformString(v, context));
            default -> doNothing();
        });

        return builder.build();
    }

    private void transformProperties(JsonValue jsonValue, Consumer<Map<String, String>> consumer, TransformerContext context) {
        JsonObject jsonObject;
        if (jsonValue instanceof JsonArray) {
            jsonObject = jsonValue.asJsonArray().getJsonObject(0);
        } else if (jsonValue instanceof JsonObject) {
            jsonObject = (JsonObject) jsonValue;
        } else {
            context.problem()
                    .unexpectedType()
                    .actual(jsonValue.getValueType())
                    .expected(OBJECT)
                    .expected(ARRAY)
                    .report();
            return;
        }
        var properties = new HashMap<String, String>();
        visitProperties(jsonObject, (k, v) -> {
            properties.put(k, transformString(v, context));
        });
        consumer.accept(properties);
    }
}

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

package org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
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
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_CONTRACT_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_DATAPLANE_METADATA;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_DATA_DESTINATION;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TRANSFER_TYPE;

public class JsonObjectToTransferRequestTransformer extends AbstractJsonLdTransformer<JsonObject, TransferRequest> {

    public JsonObjectToTransferRequestTransformer() {
        super(JsonObject.class, TransferRequest.class);
    }

    @Override
    public @Nullable TransferRequest transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = TransferRequest.Builder.newInstance();

        builder.id(nodeId(input));
        visitProperties(input, key -> switch (key) {
            case TRANSFER_REQUEST_COUNTER_PARTY_ADDRESS -> value ->
                    builder.counterPartyAddress(transformString(value, context));
            case TRANSFER_REQUEST_CONTRACT_ID -> value -> builder.contractId(transformString(value, context));
            case TRANSFER_REQUEST_DATA_DESTINATION -> value ->
                    builder.dataDestination(transformObject(value, DataAddress.class, context));
            case TRANSFER_REQUEST_CALLBACK_ADDRESSES -> value -> {
                var addresses = new ArrayList<CallbackAddress>();
                transformArrayOrObject(value, CallbackAddress.class, addresses::add, context);
                builder.callbackAddresses(addresses);
            };
            case TRANSFER_REQUEST_PRIVATE_PROPERTIES -> value ->
                    transformProperties(value, builder::privateProperties, context);
            case TRANSFER_REQUEST_PROTOCOL -> value -> builder.protocol(transformString(value, context));
            case TRANSFER_REQUEST_TRANSFER_TYPE -> value ->
                    builder.transferType(transformString(value, context));
            case TRANSFER_REQUEST_DATAPLANE_METADATA -> value ->
                    builder.dataplaneMetadata(transformObject(value, DataplaneMetadata.class, context));
            default -> doNothing();
        });

        return builder.build();
    }

    private void transformProperties(JsonValue jsonValue, Consumer<Map<String, Object>> consumer, TransformerContext context) {
        JsonObject jsonObject;
        if (jsonValue instanceof JsonArray array) {
            jsonObject = array.getJsonObject(0);
        } else if (jsonValue instanceof JsonObject object) {
            jsonObject = object;
        } else {
            context.problem()
                    .unexpectedType()
                    .actual(jsonValue.getValueType())
                    .expected(OBJECT)
                    .expected(ARRAY)
                    .report();
            return;
        }

        var properties = new HashMap<String, Object>();
        visitProperties(jsonObject, (k, v) -> properties.put(k, transformString(v, context)));
        consumer.accept(properties);
    }

}

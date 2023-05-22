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
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_ASSET_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_CONTRACT_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_DATA_DESTINATION;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_MANAGED_RESOURCES;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_PROPERTIES;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_PROTOCOL;

public class JsonObjectToTransferRequestDtoTransformer extends AbstractJsonLdTransformer<JsonObject, TransferRequestDto> {

    public JsonObjectToTransferRequestDtoTransformer() {
        super(JsonObject.class, TransferRequestDto.class);
    }

    @Override
    public @Nullable TransferRequestDto transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = TransferRequestDto.Builder.newInstance();

        builder.id(nodeId(input));
        visitProperties(input, k -> {
            switch (k) {
                case EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ADDRESS:
                    return v -> builder.connectorAddress(transformString(v, context));
                case EDC_TRANSFER_REQUEST_DTO_CONTRACT_ID:
                    return (v) -> builder.contractId(transformString(v, context));
                case EDC_TRANSFER_REQUEST_DTO_DATA_DESTINATION:
                    return (v) -> builder.dataDestination(transformObject(v, DataAddress.class, context));
                case EDC_TRANSFER_REQUEST_DTO_MANAGED_RESOURCES:
                    return (v) -> builder.managedResources(transformBoolean(v, context));
                case EDC_TRANSFER_REQUEST_DTO_PROPERTIES:
                    return (v) -> transformProperties(v, builder::properties, context);
                case EDC_TRANSFER_REQUEST_DTO_PRIVATE_PROPERTIES:
                    return (v) -> transformProperties(v, builder::privateProperties, context);
                case EDC_TRANSFER_REQUEST_DTO_PROTOCOL:
                    return (v) -> builder.protocol(transformString(v, context));
                case EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ID:
                    return (v) -> builder.connectorId(transformString(v, context));
                case EDC_TRANSFER_REQUEST_DTO_ASSET_ID:
                    return (v) -> builder.assetId(transformString(v, context));
                default:
                    return doNothing();
            }
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

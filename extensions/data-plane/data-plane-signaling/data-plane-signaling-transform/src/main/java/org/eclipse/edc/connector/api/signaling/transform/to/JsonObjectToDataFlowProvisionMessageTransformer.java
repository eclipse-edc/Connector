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

package org.eclipse.edc.connector.api.signaling.transform.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Optional;

import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.Builder;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_AGREEMENT_ID;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_CALLBACK_ADDRESS;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_DATASET_ID;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_DESTINATION;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_FLOW_TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_PARTICIPANT_ID;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_PROCESS_ID;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_PROPERTIES;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_RESPONSE_CHANNEL;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_TYPE_DESTINATION;

/**
 * Converts from a {@link JsonObject} in JSON-LD expanded form to a {@link DataFlowProvisionMessage}.
 */
public class JsonObjectToDataFlowProvisionMessageTransformer extends AbstractJsonLdTransformer<JsonObject, DataFlowProvisionMessage> {

    public JsonObjectToDataFlowProvisionMessageTransformer() {
        super(JsonObject.class, DataFlowProvisionMessage.class);
    }

    @Override
    public @Nullable DataFlowProvisionMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Builder.newInstance();

        var transferTypeDestination = object.get(EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_TYPE_DESTINATION);
        var flowType = object.get(EDC_DATA_FLOW_PROVISION_MESSAGE_FLOW_TYPE);
        if (transferTypeDestination != null && flowType != null) {
            builder.transferType(new TransferType(
                    transformString(transferTypeDestination, context),
                    FlowType.valueOf(transformString(flowType, context)),
                    transformString(object.get(EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_RESPONSE_CHANNEL), context))
            );
        } else {
            context.problem().missingProperty().property("%s - %s".formatted(EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_TYPE_DESTINATION, EDC_DATA_FLOW_PROVISION_MESSAGE_FLOW_TYPE))
                    .report();
            return null;
        }

        visitProperties(object, (s, jsonValue) -> transformProperties(s, jsonValue, builder, context));

        return builder.build();
    }

    private void transformProperties(String key, JsonValue jsonValue, Builder builder, TransformerContext context) {
        switch (key) {
            case EDC_DATA_FLOW_PROVISION_MESSAGE_PROCESS_ID -> builder.processId(transformString(jsonValue, context));
            case EDC_DATA_FLOW_PROVISION_MESSAGE_AGREEMENT_ID -> builder.agreementId(transformString(jsonValue, context));
            case EDC_DATA_FLOW_PROVISION_MESSAGE_DATASET_ID -> builder.assetId(transformString(jsonValue, context));
            case EDC_DATA_FLOW_PROVISION_MESSAGE_CALLBACK_ADDRESS ->
                    Optional.ofNullable(transformString(jsonValue, context)).map(URI::create).ifPresent(builder::callbackAddress);
            case EDC_DATA_FLOW_PROVISION_MESSAGE_PROPERTIES -> {
                var props = jsonValue.asJsonArray().getJsonObject(0);
                visitProperties(props, (k, val) -> transformProperties(k, val, builder, context));
            }
            case EDC_DATA_FLOW_PROVISION_MESSAGE_PARTICIPANT_ID ->
                    builder.participantId(transformString(jsonValue, context));
            case EDC_DATA_FLOW_PROVISION_MESSAGE_DESTINATION ->
                    builder.destination(transformObject(jsonValue, DataAddress.class, context));
            default -> builder.property(key, transformString(jsonValue, context));
        }
    }
}

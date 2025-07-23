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

package org.eclipse.edc.connector.api.signaling.transform.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
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
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_TYPE;

/**
 * Converts from a {@link DataFlowProvisionMessage} to a {@link JsonObject} in JSON-LD expanded form .
 */
public class JsonObjectFromDataFlowProvisionMessageTransformer extends AbstractJsonLdTransformer<DataFlowProvisionMessage, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectFromDataFlowProvisionMessageTransformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        super(DataFlowProvisionMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataFlowProvisionMessage message, @NotNull TransformerContext context) {
        var propertiesBuilder = jsonFactory.createObjectBuilder();
        transformProperties(message.getProperties(), propertiesBuilder, typeManager.getMapper(typeContext), context);
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, EDC_DATA_FLOW_PROVISION_MESSAGE_TYPE)
                .add(EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_TYPE_DESTINATION, message.getTransferType().destinationType())
                .add(EDC_DATA_FLOW_PROVISION_MESSAGE_FLOW_TYPE, message.getTransferType().flowType().toString())
                .add(EDC_DATA_FLOW_PROVISION_MESSAGE_AGREEMENT_ID, message.getAgreementId())
                .add(EDC_DATA_FLOW_PROVISION_MESSAGE_PROCESS_ID, message.getProcessId())
                .add(EDC_DATA_FLOW_PROVISION_MESSAGE_DATASET_ID, message.getAssetId())
                .add(EDC_DATA_FLOW_PROVISION_MESSAGE_PROPERTIES, propertiesBuilder)
                .add(EDC_DATA_FLOW_PROVISION_MESSAGE_CALLBACK_ADDRESS, message.getCallbackAddress().toString())
                .add(EDC_DATA_FLOW_PROVISION_MESSAGE_PARTICIPANT_ID, message.getParticipantId());

        if (message.getDestination() != null) {
            builder.add(EDC_DATA_FLOW_PROVISION_MESSAGE_DESTINATION, context.transform(message.getDestination(), JsonObject.class));
        }

        addIfNotNull(message.getTransferType().responseChannelType(), EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_RESPONSE_CHANNEL, builder);

        return builder.build();
    }
}

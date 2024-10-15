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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;

public class JsonObjectToTransferSuspensionMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, TransferSuspensionMessage> {

    private final ObjectMapper objectMapper;

    public JsonObjectToTransferSuspensionMessageTransformer(ObjectMapper objectMapper) {
        this(objectMapper, DSPACE_SCHEMA);
    }

    public JsonObjectToTransferSuspensionMessageTransformer(ObjectMapper objectMapper, String namespace) {
        super(JsonObject.class, TransferSuspensionMessage.class, namespace);
        this.objectMapper = objectMapper;
    }


    @Override
    public @Nullable TransferSuspensionMessage transform(@NotNull JsonObject messageObject, @NotNull TransformerContext context) {
        var builder = TransferSuspensionMessage.Builder.newInstance();

        if (!transformMandatoryString(messageObject.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM)), builder::consumerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM))
                    .report();
            return null;
        }

        if (!transformMandatoryString(messageObject.get(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM)), builder::providerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM))
                    .report();
            return null;
        }

        if (messageObject.containsKey(forNamespace(DSPACE_PROPERTY_CODE_TERM))) {
            transformString(messageObject.get(forNamespace(DSPACE_PROPERTY_CODE_TERM)), builder::code, context);
        }

        var reasons = messageObject.get(forNamespace(DSPACE_PROPERTY_REASON_TERM));
        if (reasons != null) {
            if (reasons instanceof JsonArray array && !array.isEmpty()) {
                builder.reason(array.stream().map(this::deserialize).toList());
            } else {
                context.problem()
                        .unexpectedType()
                        .type(forNamespace(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM))
                        .property(forNamespace(DSPACE_PROPERTY_REASON_TERM))
                        .actual(reasons.getValueType())
                        .expected(ARRAY)
                        .report();
            }
        }

        return builder.build();

    }

    private Object deserialize(JsonValue it) {
        try {
            return objectMapper.readValue(it.toString(), Map.class);
        } catch (JsonProcessingException e) {
            throw new EdcException("Error deserializing 'reason' field.");
        }
    }
}

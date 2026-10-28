/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.NamespacedJsonLdToModelTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;

public class JsonObjectToTransferTerminationMessageTransformer extends NamespacedJsonLdToModelTransformer<JsonObject, TransferTerminationMessage> {

    public JsonObjectToTransferTerminationMessageTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, TransferTerminationMessage.class, namespace);
    }

    @Override
    public @Nullable TransferTerminationMessage transform(@NotNull JsonObject messageObject, @NotNull TransformerContext context) {
        var builder = TransferTerminationMessage.Builder.newInstance();

        if (!transformMandatoryString(messageObject.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM)), builder::consumerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM))
                    .report();
            return null;
        }

        if (!transformMandatoryString(messageObject.get(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM)), builder::providerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM))
                    .report();
            return null;
        }

        if (messageObject.containsKey(forNamespace(DSPACE_PROPERTY_CODE_TERM))) {
            transformString(messageObject.get(forNamespace(DSPACE_PROPERTY_CODE_TERM)), builder::code, context);
        }

        var reasons = messageObject.get(forNamespace(DSPACE_PROPERTY_REASON_TERM));
        if (reasons != null) {  // optional property
            if (!(reasons instanceof JsonArray)) {
                context.problem()
                        .unexpectedType()
                        .type(forNamespace(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM))
                        .property(forNamespace(DSPACE_PROPERTY_REASON_TERM))
                        .actual(reasons.getValueType())
                        .expected(ARRAY)
                        .report();
            } else {
                var array = (JsonArray) reasons;
                if (array.size() > 0) {
                    builder.reason(reasonText(array));
                }
            }
        }

        return builder.build();

    }

    /**
     * The reasons as text: a plain-text reason, expanded to a value object ({@code {"@value": "..."}}), contributes its
     * value, and several are joined with "; ". When any reason is structured, all of them are kept as JSON.
     */
    private String reasonText(JsonArray reasons) {
        var texts = reasons.stream().map(this::valueText).toList();
        return texts.contains(null) ? reasons.toString() : String.join("; ", texts);
    }

    private @Nullable String valueText(JsonValue reason) {
        if (reason instanceof JsonString text) {
            return text.getString();
        }
        if (reason instanceof JsonObject object && object.get(VALUE) instanceof JsonString text) {
            return text.getString();
        }
        return null;
    }
}

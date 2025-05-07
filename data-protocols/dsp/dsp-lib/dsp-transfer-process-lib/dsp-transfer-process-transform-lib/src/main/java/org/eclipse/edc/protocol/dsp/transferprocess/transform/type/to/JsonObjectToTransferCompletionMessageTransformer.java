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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM;

public class JsonObjectToTransferCompletionMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, TransferCompletionMessage> {

    public JsonObjectToTransferCompletionMessageTransformer() {
        this(DSP_NAMESPACE_V_08);
    }

    public JsonObjectToTransferCompletionMessageTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, TransferCompletionMessage.class, namespace);
    }

    @Override
    public @Nullable TransferCompletionMessage transform(@NotNull JsonObject messageObject, @NotNull TransformerContext context) {
        var builder = TransferCompletionMessage.Builder.newInstance();

        if (!transformMandatoryString(messageObject.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM)), builder::consumerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM))
                    .report();
            return null;
        }

        if (!transformMandatoryString(messageObject.get(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM)), builder::providerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM))
                    .report();
            return null;
        }

        return builder.build();

    }
}

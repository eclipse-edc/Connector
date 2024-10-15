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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessAck;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE_TERM;

/**
 * Create a {@link TransferProcessAck} from {@link JsonObject}
 */
public class JsonObjectToTransferProcessAckTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, TransferProcessAck> {

    public JsonObjectToTransferProcessAckTransformer() {
        this(DSPACE_SCHEMA);
    }

    public JsonObjectToTransferProcessAckTransformer(String namespace) {
        super(JsonObject.class, TransferProcessAck.class, namespace);
    }

    @Override
    public @Nullable TransferProcessAck transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = TransferProcessAck.Builder.newInstance();
        transformMandatoryString(jsonObject.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM)), builder::consumerPid, context);
        transformMandatoryString(jsonObject.get(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM)), builder::providerPid, context);

        return builder
                .state(transformString(jsonObject.get(forNamespace(DSPACE_PROPERTY_STATE_TERM)), context))
                .build();
    }
}

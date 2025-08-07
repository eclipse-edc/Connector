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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_PROCESS_TERM;

public class JsonObjectFromTransferProcessTransformer extends AbstractNamespaceAwareJsonLdTransformer<TransferProcess, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferProcessTransformer(JsonBuilderFactory jsonBuilderFactory, JsonLdNamespace namespace) {
        super(TransferProcess.class, JsonObject.class, namespace);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferProcess transferProcess, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder()
                .add(ID, transferProcess.getId())
                .add(TYPE, forNamespace(DSPACE_TYPE_TRANSFER_PROCESS_TERM))
                .add(forNamespace(DSPACE_PROPERTY_STATE_TERM), TransferProcessStates.from(transferProcess.getState()).name());

        if (transferProcess.getType() == TransferProcess.Type.PROVIDER) {
            builder.add(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), transferProcess.getId());
            addIfNotNull(transferProcess.getCorrelationId(), forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), builder);
        } else {
            builder.add(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), transferProcess.getId());
            addIfNotNull(transferProcess.getCorrelationId(), forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), builder);
        }

        return builder.build();
    }

}

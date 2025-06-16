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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from;

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
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_PROCESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_REQUESTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_STARTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_SUSPENDED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM;

public class JsonObjectFromTransferProcessV2024Transformer extends AbstractNamespaceAwareJsonLdTransformer<TransferProcess, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferProcessV2024Transformer(JsonBuilderFactory jsonBuilderFactory) {
        this(jsonBuilderFactory, DSP_NAMESPACE_V_2024_1);
    }

    public JsonObjectFromTransferProcessV2024Transformer(JsonBuilderFactory jsonBuilderFactory, JsonLdNamespace namespace) {
        super(TransferProcess.class, JsonObject.class, namespace);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferProcess transferProcess, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder()
                .add(ID, transferProcess.getId())
                .add(TYPE, forNamespace(DSPACE_TYPE_TRANSFER_PROCESS_TERM))
                .add(forNamespace(DSPACE_PROPERTY_STATE_TERM), createId(jsonBuilderFactory, state(transferProcess.getState(), transferProcess.getErrorDetail(), context)));

        if (transferProcess.getType() == TransferProcess.Type.PROVIDER) {
            builder.add(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), createId(jsonBuilderFactory, transferProcess.getId()));
            addIdIfNotNull(transferProcess.getCorrelationId(), forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), jsonBuilderFactory, builder);
        } else {
            builder.add(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), createId(jsonBuilderFactory, transferProcess.getId()));
            addIdIfNotNull(transferProcess.getCorrelationId(), forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), jsonBuilderFactory, builder);
        }

        return builder.build();
    }

    private String state(Integer state, String errorDetails, TransformerContext context) {
        var transferProcessState = TransferProcessStates.from(state);
        if (transferProcessState == null) {
            context.problem()
                    .nullProperty()
                    .type(TransferProcess.class)
                    .property(forNamespace(DSPACE_PROPERTY_STATE_TERM))
                    .report();
            return null;
        }

        return switch (transferProcessState) {
            case INITIAL, REQUESTING, REQUESTED -> forNamespace(DSPACE_VALUE_TRANSFER_STATE_REQUESTED_TERM);
            case STARTING, STARTED -> forNamespace(DSPACE_VALUE_TRANSFER_STATE_STARTED_TERM);
            case SUSPENDING, SUSPENDED, SUSPENDING_REQUESTED, RESUMING, RESUMED ->
                    forNamespace(DSPACE_VALUE_TRANSFER_STATE_SUSPENDED_TERM);
            case COMPLETING, COMPLETING_REQUESTED, COMPLETED ->
                    forNamespace(DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM);

            case DEPROVISIONING, DEPROVISIONING_REQUESTED, DEPROVISIONED -> {
                if (errorDetails != null) {
                    yield forNamespace(DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM);
                } else {
                    yield forNamespace(DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM);
                }
            }
            case TERMINATING, TERMINATING_REQUESTED, TERMINATED ->
                    forNamespace(DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM);
            default -> {
                context.problem()
                        .unexpectedType()
                        .type(TransferProcess.class)
                        .property("state")
                        .actual(transferProcessState.toString())
                        .expected(TransferProcessStates.class)
                        .report();
                yield null;
            }
        };
    }
}

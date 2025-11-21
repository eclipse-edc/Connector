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

package org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_ASSET_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CONTRACT_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CORRELATION_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_DATAPLANE_METADATA;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_DATA_DESTINATION;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_ERROR_DETAIL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_STATE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_STATE_TIMESTAMP;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TRANSFER_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromTransferProcessTransformer extends AbstractJsonLdTransformer<TransferProcess, JsonObject> {

    private final JsonBuilderFactory builderFactory;

    public JsonObjectFromTransferProcessTransformer(JsonBuilderFactory builderFactory) {
        super(TransferProcess.class, JsonObject.class);
        this.builderFactory = builderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferProcess input, @NotNull TransformerContext context) {
        var callbackAddresses = input.getCallbackAddresses().stream()
                .map(it -> context.transform(it, JsonObject.class))
                .collect(toJsonArray());

        var builder = builderFactory.createObjectBuilder()
                .add(ID, input.getId())
                .add(TYPE, TRANSFER_PROCESS_TYPE)
                .add(TRANSFER_PROCESS_STATE, TransferProcessStates.from(input.getState()).name())
                .add(TRANSFER_PROCESS_STATE_TIMESTAMP, input.getStateTimestamp())
                .add(TRANSFER_PROCESS_TYPE_TYPE, input.getType().name())
                .add(TRANSFER_PROCESS_CALLBACK_ADDRESSES, callbackAddresses);

        addIfNotNull(input.getCorrelationId(), TRANSFER_PROCESS_CORRELATION_ID, builder);
        addIfNotNull(input.getAssetId(), TRANSFER_PROCESS_ASSET_ID, builder);
        addIfNotNull(input.getContractId(), TRANSFER_PROCESS_CONTRACT_ID, builder);
        addIfNotNull(input.getTransferType(), TRANSFER_PROCESS_TRANSFER_TYPE, builder);
        addIfNotNull(input.getErrorDetail(), TRANSFER_PROCESS_ERROR_DETAIL, builder);
        Optional.ofNullable(input.getDataDestination()).map(it -> context.transform(it, JsonObject.class))
                .ifPresent(it -> builder.add(TRANSFER_PROCESS_DATA_DESTINATION, it));
        Optional.ofNullable(input.getDataplaneMetadata()).map(it -> context.transform(it, JsonObject.class))
                .ifPresent(it -> builder.add(TRANSFER_PROCESS_DATAPLANE_METADATA, it));

        return builder.build();
    }

}

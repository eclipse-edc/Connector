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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_DATA_DESTINATION;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_DATA_REQUEST;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_ERROR_DETAIL;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_STATE;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_STATE_TIMESTAMP;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_TYPE;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_TYPE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromTransferProcessDtoTransformer extends AbstractJsonLdTransformer<TransferProcessDto, JsonObject> {

    private final JsonBuilderFactory builderFactory;
    private final ObjectMapper mapper;

    public JsonObjectFromTransferProcessDtoTransformer(JsonBuilderFactory builderFactory, ObjectMapper mapper) {
        super(TransferProcessDto.class, JsonObject.class);
        this.builderFactory = builderFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferProcessDto input, @NotNull TransformerContext context) {
        var callbackAddresses = input.getCallbackAddresses().stream()
                .map(it -> context.transform(it, JsonObject.class))
                .collect(collectingAndThen(toList(), l -> builderFactory.createArrayBuilder(l).build()));

        var builder = builderFactory.createObjectBuilder()
                .add(ID, input.getId())
                .add(TYPE, EDC_TRANSFER_PROCESS_DTO_TYPE)
                .add(EDC_TRANSFER_PROCESS_DTO_STATE, input.getState())
                .add(EDC_TRANSFER_PROCESS_DTO_STATE_TIMESTAMP, input.getStateTimestamp())
                .add(EDC_TRANSFER_PROCESS_DTO_TYPE_TYPE, input.getType())
                .add(EDC_TRANSFER_PROCESS_DTO_CALLBACK_ADDRESSES, callbackAddresses)
                .add(EDC_TRANSFER_PROCESS_DTO_DATA_DESTINATION, context.transform(input.getDataDestination(), JsonObject.class))
                .add(EDC_TRANSFER_PROCESS_DTO_DATA_REQUEST, context.transform(input.getDataRequest(), JsonObject.class));

        Optional.ofNullable(input.getErrorDetail()).ifPresent(it -> builder.add(EDC_TRANSFER_PROCESS_DTO_ERROR_DETAIL, it));

        transformProperties(input.getProperties(), builder, mapper, context);

        return builder.build();
    }

}

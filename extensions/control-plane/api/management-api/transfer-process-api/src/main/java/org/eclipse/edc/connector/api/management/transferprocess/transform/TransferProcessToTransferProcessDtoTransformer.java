/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.transferprocess.transform;

import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class TransferProcessToTransferProcessDtoTransformer implements DtoTransformer<TransferProcess, TransferProcessDto> {

    @Override
    public Class<TransferProcess> getInputType() {
        return TransferProcess.class;
    }

    @Override
    public Class<TransferProcessDto> getOutputType() {
        return TransferProcessDto.class;
    }

    @Override
    public @Nullable TransferProcessDto transform(@NotNull TransferProcess object, @NotNull TransformerContext context) {
        var dataRequest = object.getDataRequest();
        var dataRequestProperties = Optional.ofNullable(dataRequest).map(DataRequest::getDataDestination).map(DataAddress::getProperties).orElseGet(Collections::emptyMap);
        return TransferProcessDto.Builder.newInstance()
                .id(object.getId())
                .type(object.getType().name())
                .state(getState(object.getState(), context))
                .stateTimestamp(object.getStateTimestamp())
                .errorDetail(object.getErrorDetail())
                .createdAt(object.getCreatedAt())
                .updatedAt(object.getUpdatedAt())
                .dataRequest(context.transform(dataRequest, DataRequestDto.class))
                .properties(object.getProperties())
                .callbackAddresses(object.getCallbackAddresses().stream().map(it -> context.transform(it, CallbackAddressDto.class)).collect(toList()))
                .dataDestination(
                        DataAddressDto.Builder.newInstance()
                                .properties(dataRequestProperties)
                                .build())
                .build();
    }

    private String getState(int value, TransformerContext context) {
        var result = TransferProcessStates.from(value);
        if (result == null) {
            context.problem()
                    .unexpectedType()
                    .type(TransferProcess.class)
                    .property("state")
                    .expected(TransferProcessStates.class)
                    .actual(String.valueOf(value))
                    .report();
            return null;
        }
        return result.name();
    }
}

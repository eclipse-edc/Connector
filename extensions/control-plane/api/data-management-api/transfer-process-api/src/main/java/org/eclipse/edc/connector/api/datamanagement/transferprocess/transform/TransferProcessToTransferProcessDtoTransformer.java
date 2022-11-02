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

package org.eclipse.edc.connector.api.datamanagement.transferprocess.transform;

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.datamanagement.transferprocess.model.DataAddressInformationDto;
import org.eclipse.edc.connector.api.datamanagement.transferprocess.model.DataRequestDto;
import org.eclipse.edc.connector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public @Nullable TransferProcessDto transform(@Nullable TransferProcess object, @NotNull TransformerContext context) {
        if (object == null) {
            return null;
        }
        return TransferProcessDto.Builder.newInstance()
                .id(object.getId())
                .type(object.getType().name())
                .state(getState(object.getState(), context))
                .stateTimestamp(object.getStateTimestamp())
                .errorDetail(object.getErrorDetail())
                .createdAt(object.getCreatedAt())
                .updatedAt(object.getUpdatedAt())
                .dataRequest(context.transform(object.getDataRequest(), DataRequestDto.class))
                .dataDestination(
                        DataAddressInformationDto.Builder.newInstance()
                                .properties(object.getDataRequest().getDataDestination().getProperties())
                                .build())
                .build();
    }

    private String getState(int value, TransformerContext context) {
        var result = TransferProcessStates.from(value);
        if (result == null) {
            context.reportProblem("Invalid value for TransferProcess.state");
            return null;
        }
        return result.name();
    }
}

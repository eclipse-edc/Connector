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

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.DataRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
                .errorDetail(object.getErrorDetail())
                .dataRequest(context.transform(object.getDataRequest(), DataRequestDto.class))
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

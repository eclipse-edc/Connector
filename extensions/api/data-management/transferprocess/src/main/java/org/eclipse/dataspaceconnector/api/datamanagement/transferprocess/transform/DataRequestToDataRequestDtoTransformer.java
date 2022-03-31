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
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DataRequestToDataRequestDtoTransformer implements DtoTransformer<DataRequest, DataRequestDto> {

    @Override
    public Class<DataRequest> getInputType() {
        return DataRequest.class;
    }

    @Override
    public Class<DataRequestDto> getOutputType() {
        return DataRequestDto.class;
    }

    @Override
    public @Nullable DataRequestDto transform(@Nullable DataRequest object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }
        return DataRequestDto.Builder.newInstance()
                .assetId(object.getAssetId())
                .contractId(object.getContractId())
                .connectorId(object.getConnectorId())
                .build();
    }
}

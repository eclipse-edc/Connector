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

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public @Nullable DataRequestDto transform(@NotNull DataRequest object, @NotNull TransformerContext context) {
        return DataRequestDto.Builder.newInstance()
                .id(object.getId())
                .assetId(object.getAssetId())
                .contractId(object.getContractId())
                .connectorId(object.getConnectorId())
                .build();
    }
}

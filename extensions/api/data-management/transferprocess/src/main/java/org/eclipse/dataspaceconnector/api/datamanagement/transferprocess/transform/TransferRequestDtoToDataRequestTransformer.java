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

import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TransferRequestDtoToDataRequestTransformer implements DtoTransformer<TransferRequestDto, DataRequest> {

    @Override
    public Class<TransferRequestDto> getInputType() {
        return TransferRequestDto.class;
    }

    @Override
    public Class<DataRequest> getOutputType() {
        return DataRequest.class;
    }

    @Override
    public @Nullable DataRequest transform(@Nullable TransferRequestDto object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }
        return DataRequest.Builder.newInstance()
                .assetId(object.getAssetId())
                .connectorId(object.getConnectorId())
                .dataDestination(object.getDataDestination())
                .connectorAddress(object.getConnectorAddress())
                .contractId(object.getContractId())
                .transferType(object.getTransferType())
                .destinationType(object.getDataDestination().getType())
                .properties(object.getProperties())
                .managedResources(object.isManagedResources())
                .protocol(object.getProtocol())
                .dataDestination(object.getDataDestination())
                .build();
    }
}

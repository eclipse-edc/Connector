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
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.util.UUID;

import static org.mockito.Mockito.mock;

class DataRequestTransformerTestData {

    TransformerContext context = mock(TransformerContext.class);
    String assetId = UUID.randomUUID().toString();
    String contractId = UUID.randomUUID().toString();
    String connectorId = UUID.randomUUID().toString();

    DataRequest entity = DataRequest.Builder.newInstance()
            .assetId(assetId)
            .contractId(contractId)
            .connectorId(connectorId)
            .dataDestination(DataAddress.Builder.newInstance().type("test-type").build())
            .build();

    DataRequestDto dto = DataRequestDto.Builder.newInstance()
            .assetId(assetId)
            .contractId(contractId)
            .connectorId(connectorId)
            .build();
}
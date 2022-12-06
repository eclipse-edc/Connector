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

import org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;

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

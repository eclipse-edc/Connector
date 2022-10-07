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

import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.DataAddressInformationDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.DataRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public class TransferProcessTransformerTestData {

    DtoTransformerRegistry registry = mock(DtoTransformerRegistry.class);
    TransformerContext context = new DtoTransformerRegistryImpl.DtoTransformerContext(registry);
    String id = UUID.randomUUID().toString();
    TransferProcess.Type type = TransferProcess.Type.CONSUMER;
    TransferProcessStates state = TransferProcessStates.REQUESTING;
    long stateTimestamp = Instant.now().toEpochMilli();
    long createdTimestamp = Instant.now().toEpochMilli();
    String errorDetail = "test-error";

    Map<String, String> dataDestinationProperties = Map.of("key1", "value1");
    String dataDestinationType = "test-type";
    DataAddress.Builder dataDestination = DataAddress.Builder.newInstance().type(dataDestinationType).properties(dataDestinationProperties);
    DataRequest dataRequest = DataRequest.Builder.newInstance()
            .dataDestination(dataDestination.build())
            .build();
    public TransferProcess.Builder entity = TransferProcess.Builder.newInstance()
            .id(id)
            .type(type)
            .state(state.code())
            .stateTimestamp(stateTimestamp)
            .createdTimestamp(createdTimestamp)
            .errorDetail(errorDetail)
            .dataRequest(dataRequest);
    DataRequestDto dataRequestDto = DataRequestDto.Builder.newInstance().build();
    TransferProcessDto.Builder dto = TransferProcessDto.Builder.newInstance()
            .id(id)
            .type(type.name())
            .state(state.name())
            .stateTimestamp(stateTimestamp)
            .createdTimestamp(createdTimestamp)
            .errorDetail(errorDetail)
            .dataRequest(dataRequestDto)
            .dataDestination(
                    DataAddressInformationDto.Builder.newInstance()
                            .properties(mapWith(dataDestinationProperties, "type", dataDestinationType))
                            .build());

    private Map<String, String> mapWith(Map<String, String> sourceMap, String key, String value) {
        var newMap = new HashMap<>(sourceMap);
        newMap.put(key, value);
        return newMap;
    }
}
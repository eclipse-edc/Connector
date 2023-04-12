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

import org.eclipse.edc.connector.api.management.transferprocess.model.DataAddressInformationDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TransferProcessTransformerTestData {

    public CallbackAddress callbackAddress = CallbackAddress.Builder.newInstance().uri("local://test").build();
    String id = UUID.randomUUID().toString();
    TransferProcess.Type type = TransferProcess.Type.CONSUMER;
    TransferProcessStates state = TransferProcessStates.values()[ThreadLocalRandom.current().nextInt(TransferProcessStates.values().length)];
    long stateTimestamp = System.currentTimeMillis();
    long createdTimestamp = ThreadLocalRandom.current().nextLong();
    String errorDetail = "test error detail";
    Map<String, String> dataDestinationProperties = Map.of("key1", "value1");
    String dataDestinationType = "test-destination-type";
    DataAddress.Builder dataDestination = DataAddress.Builder.newInstance().type(dataDestinationType).properties(dataDestinationProperties);
    DataRequest dataRequest = DataRequest.Builder.newInstance()
            .dataDestination(dataDestination.build())
            .build();
    public TransferProcess.Builder entity = TransferProcess.Builder.newInstance()
            .id(id)
            .type(type)
            .state(state.code())
            .stateTimestamp(stateTimestamp)
            .createdAt(createdTimestamp)
            .errorDetail(errorDetail)
            .dataRequest(dataRequest)
            .callbackAddresses(List.of(callbackAddress));
    DataRequestDto dataRequestDto = DataRequestDto.Builder.newInstance().build();
    TransferProcessDto.Builder dto = TransferProcessDto.Builder.newInstance()
            .id(id)
            .type(type.name())
            .state(state.name())
            .stateTimestamp(stateTimestamp)
            .errorDetail(errorDetail)
            .dataRequest(dataRequestDto)
            .createdAt(createdTimestamp)
            .updatedAt(createdTimestamp)
            .callbackAddresses(List.of(callbackAddress))
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

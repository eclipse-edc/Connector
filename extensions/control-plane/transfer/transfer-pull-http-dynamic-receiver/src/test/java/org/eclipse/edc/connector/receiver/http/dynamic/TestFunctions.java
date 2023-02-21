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

package org.eclipse.edc.connector.receiver.http.dynamic;

import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.connector.receiver.http.dynamic.HttpDynamicEndpointDataReferenceReceiver.HTTP_RECEIVER_ENDPOINT;

public class TestFunctions {

    public static TransferProcess createTransferProcess(String id, Map<String, String> properties) {
        return TransferProcess.Builder.newInstance()
                .id(id)
                .state(TransferProcessStates.STARTED.code())
                .type(TransferProcess.Type.CONSUMER)
                .contentDataAddress(HttpDataAddress.Builder.newInstance()
                        .baseUrl("http://localhost:8080/test")
                        .build())
                .dataRequest(DataRequest.Builder.newInstance()
                        .destinationType("HttpProxy")
                        .build())
                .properties(properties)
                .build();
    }

    public static TransferProcess createTransferProcess(String id) {
        return createTransferProcess(id, new HashMap<>());
    }


    public static Map<String, String> transferProperties(String url) {
        return new HashMap<>(Map.of(HTTP_RECEIVER_ENDPOINT, url));
    }

    public static Map<String, String> transferPropertiesWithAuth(String url, String key, String token) {
        return new HashMap<>(Map.of(HTTP_RECEIVER_ENDPOINT, url));
    }
}

/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;

import static okhttp3.Protocol.HTTP_2;

public class HttpTestFixtures {

    public static DataFlowRequest.Builder createRequest(String type) {
        return DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .transferType(TransferType.Builder.transferType().contentType("application/octet-stream").build())
                .sourceDataAddress(DataAddress.Builder.newInstance().type(type).build())
                .destinationDataAddress(DataAddress.Builder.newInstance().build());
    }

    public static Response.Builder createHttpResponse(){
        var body = ResponseBody.create("{}", MediaType.get("application/json"));
        var request = new Request.Builder().url("https://test.com").build();
        return new Response.Builder().code(200).body(body).request(request).protocol(HTTP_2).message("");
    }

    private HttpTestFixtures() {
    }
}

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
package org.eclipse.dataspaceconnector.transfer.functions.core.flow.http;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static okhttp3.Protocol.HTTP_1_1;
import static org.easymock.EasyMock.createNiceMock;
import static org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus.FATAL_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies HTTP transfer function flow.
 */
class HttpFunctionDataFlowControllerTest {
    private HttpFunctionDataFlowController flowController;
    private OkHttpClient httpClient;
    private Interceptor interceptor;

    @Test
    void verifyOkResponse() throws IOException {
        Interceptor delegate = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(200)
                .body(ResponseBody.create("", MediaType.get("application/json"))).message("ok")
                .build();

        //noinspection ConstantConditions
        EasyMock.expect(interceptor.intercept(EasyMock.isA(Interceptor.Chain.class))).andDelegateTo(delegate);
        EasyMock.replay(interceptor);

        var dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).build();
        assertEquals(DataFlowInitiateResponse.OK, flowController.initiateFlow(dataRequest));

        EasyMock.verify(interceptor);
    }

    @Test
    void verifyRetryErrorResponse() throws IOException {
        Interceptor delegate = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(500)
                .body(ResponseBody.create("", MediaType.get("application/json"))).message("ok")
                .build();

        //noinspection ConstantConditions
        EasyMock.expect(interceptor.intercept(EasyMock.isA(Interceptor.Chain.class))).andDelegateTo(delegate);
        EasyMock.replay(interceptor);

        var dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).build();
        assertEquals(ERROR_RETRY, flowController.initiateFlow(dataRequest).getStatus());

        EasyMock.verify(interceptor);
    }

    @Test
    void verifyFatalErrorResponse() throws IOException {
        Interceptor delegate = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(400)
                .body(ResponseBody.create("", MediaType.get("application/json"))).message("ok")
                .build();

        //noinspection ConstantConditions
        EasyMock.expect(interceptor.intercept(EasyMock.isA(Interceptor.Chain.class))).andDelegateTo(delegate);
        EasyMock.replay(interceptor);

        var dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).build();
        assertEquals(FATAL_ERROR, flowController.initiateFlow(dataRequest).getStatus());

        EasyMock.verify(interceptor);
    }


    @BeforeEach
    void setUp() {
        interceptor = EasyMock.createMock(Interceptor.class);

        var typeManager = new TypeManager();
        httpClient = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        var configuration = HttpFunctionConfiguration.Builder.newInstance()
                .transferEndpoint("https://localhost:9090/check")
                .clientSupplier(() -> httpClient)
                .monitor(createNiceMock(Monitor.class))
                .typeManager(typeManager)
                .build();
        flowController = new HttpFunctionDataFlowController(configuration);
    }


}

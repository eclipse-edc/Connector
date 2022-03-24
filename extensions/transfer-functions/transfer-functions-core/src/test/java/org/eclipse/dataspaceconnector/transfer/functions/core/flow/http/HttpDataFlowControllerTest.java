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
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.testOkHttpClient;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.FATAL_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class HttpDataFlowControllerTest {
    private HttpDataFlowController flowController;
    private OkHttpClient httpClient;

    @BeforeEach
    void setUp() {
        var typeManager = new TypeManager();
        var configuration = HttpDataFlowConfiguration.Builder.newInstance()
                .transferEndpoint("https://localhost:9090/check")
                .clientSupplier(() -> httpClient)
                .monitor(mock(Monitor.class))
                .typeManager(typeManager)
                .build();
        var addressResolver = mock(DataAddressResolver.class);
        when(addressResolver.resolveForAsset(any())).thenReturn(DataAddress.Builder.newInstance().type("test").build());
        flowController = new HttpDataFlowController(configuration, addressResolver);
    }

    @Test
    void verifyOkResponse() {
        Interceptor delegate = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(200)
                .body(ResponseBody.create("", MediaType.get("application/json"))).message("ok")
                .build();

        httpClient = testOkHttpClient().newBuilder().addInterceptor(delegate).build();

        var dataRequest = createDataRequest();
        var policy = Policy.Builder.newInstance().build();

        assertThat(flowController.initiateFlow(dataRequest, policy).succeeded()).isTrue();
    }

    @Test
    void verifyRetryErrorResponse() {
        Interceptor delegate = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(500)
                .body(ResponseBody.create("", MediaType.get("application/json"))).message("ok")
                .build();

        httpClient = testOkHttpClient().newBuilder().addInterceptor(delegate).build();

        var dataRequest = createDataRequest();
        var policy = Policy.Builder.newInstance().build();

        assertEquals(ERROR_RETRY, flowController.initiateFlow(dataRequest, policy).getFailure().status());
    }

    @Test
    void verifyFatalErrorResponse() {
        Interceptor delegate = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(400)
                .body(ResponseBody.create("", MediaType.get("application/json"))).message("ok")
                .build();

        httpClient = testOkHttpClient().newBuilder().addInterceptor(delegate).build();

        var dataRequest = createDataRequest();
        var policy = Policy.Builder.newInstance().build();

        assertEquals(FATAL_ERROR, flowController.initiateFlow(dataRequest, policy).getFailure().status());
    }

    private DataRequest createDataRequest() {
        return DataRequest.Builder.newInstance()
                .dataDestination(DataAddress.Builder.newInstance().type("test").build())
                .processId(UUID.randomUUID().toString())
                .build();
    }

}

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
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static okhttp3.Protocol.HTTP_1_1;
import static org.easymock.EasyMock.createNiceMock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verfies HTTP status checking.
 */
class HttpStatusCheckerTest {
    private HttpStatusChecker checker;
    private TypeManager typeManager;
    private OkHttpClient httpClient;
    private Interceptor interceptor;

    @Test
    void verifyCompleted() throws IOException {
        Interceptor delegate = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(200)
                .body(ResponseBody.create(typeManager.writeValueAsString(true), MediaType.get("application/json"))).message("ok")
                .build();

        //noinspection ConstantConditions
        EasyMock.expect(interceptor.intercept(EasyMock.isA(Interceptor.Chain.class))).andDelegateTo(delegate);
        EasyMock.replay(interceptor);

        assertTrue(checker.isComplete(TransferProcess.Builder.newInstance().id("123").build(), Collections.emptyList()));

        EasyMock.verify(interceptor);
    }

    @Test
    void verifyNotCompleted() throws IOException {
        Interceptor delegate = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(200)
                .body(ResponseBody.create(typeManager.writeValueAsString(false), MediaType.get("application/json"))).message("ok")
                .build();

        //noinspection ConstantConditions
        EasyMock.expect(interceptor.intercept(EasyMock.isA(Interceptor.Chain.class))).andDelegateTo(delegate);
        EasyMock.replay(interceptor);

        assertFalse(checker.isComplete(TransferProcess.Builder.newInstance().id("123").build(), Collections.emptyList()));

        EasyMock.verify(interceptor);
    }

    @Test
    void verifyServerError() throws IOException {
        Interceptor delegate = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create(typeManager.writeValueAsString(false), MediaType.get("txt/html"))).message("ok")
                .build();

        //noinspection ConstantConditions
        EasyMock.expect(interceptor.intercept(EasyMock.isA(Interceptor.Chain.class))).andDelegateTo(delegate);
        EasyMock.replay(interceptor);

        assertFalse(checker.isComplete(TransferProcess.Builder.newInstance().id("123").build(), Collections.emptyList()));

        EasyMock.verify(interceptor);
    }


    @BeforeEach
    void setUp() {
        interceptor = EasyMock.createMock(Interceptor.class);

        typeManager = new TypeManager();
        httpClient = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        var configuration = HttpFunctionConfiguration.Builder.newInstance()
                .checkEndpoint("https://localhost:9090/check")
                .clientSupplier(() -> httpClient)
                .monitor(createNiceMock(Monitor.class))
                .typeManager(typeManager)
                .build();
        checker = new HttpStatusChecker(configuration);
    }
}

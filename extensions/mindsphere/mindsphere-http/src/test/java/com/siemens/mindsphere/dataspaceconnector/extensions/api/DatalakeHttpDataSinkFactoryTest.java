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

package com.siemens.mindsphere.dataspaceconnector.extensions.api;

import com.siemens.mindsphere.datalake.edc.http.dataplane.DatalakeHttpDataSinkFactory;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.TYPE;
import static org.mockito.Mockito.mock;

class DatalakeHttpDataSinkFactoryTest {
    private DatalakeHttpDataSinkFactory factory;
    private OkHttpClient httpClient;

    @Disabled("Use mocked okclient instead")
    @Test
    void verifyUpload() throws InterruptedException, ExecutionException {
        var dataAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, "https://datalake-prod-a-castidev-1638992902776.s3.eu-central-1.amazonaws.com/data/ten%3Dcastidev/apache-groovy-sdk-4.0.2.zip?X-Amz-Security-Token=FwoGZXIvYXdzEJv%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaDBi3LtIM2X0lrQG9GyK3BKI8WNRZKSbKGEPMtuspML2BCrWocEJcoNF%2BczjKLu%2Frk3k8OXriDb4jwkAxpELuM%2FX3nK12NB%2BvbgPk4iEXDC6Yq0wvme1KopjJVW9hrmpBVdH6K5omSaggW1gNGUUiwJh%2BxL2bGOo%2Bq4B5Wre6AKmGSf6bIT3gkp3iJL5J4NlFDLNvqirh9RExuJrT0RFlJSLTdHDZuHMmQQLIucyjxrA0ChfqxLwwToFyPgUo3i%2FA0%2FcfaeQTWZetnRT%2F0o4dg6mF0BYnYcG1yiFmt7hRYiVuFUam2ezE1AY8FD4PWcX2MJ9qqO6f2nKGpDEqFDPa2U3Bzw7lLCfNn%2B1EeO%2BS2MMq3J295oB79GXHK09%2FIvswrClpNdmNF05vFu1gLoGqaOlxwm7CWfKgTPB0ddtaJCJKAoOr0hvslg4ne35eYNKARLoWGCDz%2F6MJHRgC8J2R81RE3DE%2FXMFUjn%2FTJVNOLv8CUQrjSE5ePVt6vzZzeStzPnf2mx6MR8ajZpfwbmY2HzrpRoxRMYDAXdx2hz4Kf38JzFPAip09QIdCZjGp%2BFA3GTmUtxyPIfHSfuLZ8vN1lHN2G3HWSi2oo8uaWU3Hn%2B%2BrmQt1D3%2BR4HewG6HPJF8WTt9Mvbu5WtOLmI%2B6O8YcgrVwcx%2FWxyqsSwLktkZ5B%2BxpBRKCFjYXF4CUbX42Y72iiWbjz5p5O0DWtEHcEdhSwJAO5EqF9yjbg5pleMZmWh4JmUze%2BeeAJmGriEia6QkQzxBD9ifxgiiG1bKUBjIr6oEYyCwyYcsqdcmFcMbH9ywvEvcI01J8bGTXeNYmD0Xppbc0ydj9hJrM5Q%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20220524T100147Z&X-Amz-SignedHeaders=host&X-Amz-Expires=7200&X-Amz-Credential=ASIAWX7P4S4DYQKEBE44%2F20220524%2Feu-central-1%2Fs3%2Faws4_request&X-Amz-Signature=9e577e60febfdfd652bcf654f5cf6b94321c9cfad9337edf4880a47b46b8cec3")
                .build();

        var validRequest = createRequest(TYPE).destinationDataAddress(dataAddress).build();

        var sink = factory.createSink(validRequest);

        var result = sink.transfer(new InputStreamDataSource("test", new ByteArrayInputStream("test".getBytes()))).get();
        //var result = sink.transfer(new InputStreamDataSource("test", new FileInputStream("D:\\Kits\\apache-groovy-sdk-4.0.2.zip"))).get();

        assertThat(result.failed()).isFalse();
    }

    public static DataFlowRequest.Builder createRequest(String type) {
        return createRequest(
                Map.of(DataFlowRequestSchema.METHOD, "PUT"),
                createDataAddress(type, Collections.emptyMap()).build(),
                createDataAddress(type, Collections.emptyMap()).build()
        );
    }

    public static DataFlowRequest.Builder createRequest(Map<String, String> properties, DataAddress source, DataAddress destination) {
        return DataFlowRequest.Builder.newInstance()
                .id("546754756")
                .processId("86786554")
                .properties(properties)
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .trackable(true);
    }

    public static DataAddress.Builder createDataAddress(String type, Map<String, String> properties) {
        return DataAddress.Builder.newInstance()
                .type(type)
                .properties(properties);
    }

    @BeforeEach
    void setUp() {
        httpClient = new OkHttpClient();
        factory = new DatalakeHttpDataSinkFactory(httpClient, Executors.newFixedThreadPool(1), 1, mock(Monitor.class));
    }


}

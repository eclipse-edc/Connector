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

class HttpDataSinkFactoryTest {
    private HttpDataSinkFactory factory;
    private OkHttpClient httpClient;

    @Disabled("Use mocked okclient instead")
    @Test
    void verifyUpload() throws InterruptedException, ExecutionException {
        var dataAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, "https://datalake-prod-a-castidev-1638992902776.s3.eu-central-1.amazonaws.com/data/ten%3Dcastidev/onboard.md?X-Amz-Security-Token=FwoGZXIvYXdzEIb%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaDMdNwzGQWwbgj2Uf7yK3BPSjLwEGjPo1rO6Mh53DLY%2Fo1nuF4Zef5K08seAd8bpEuAzLNyl%2FLzA5jIDdOXqnMa0uF%2FLF%2F4ZFSOU%2BKqIcy8sl%2F7DAN45eATqXA%2Fe26%2BiEbl5uBQa%2B9zhGQzIbmz09b2GBE9htwsSC5H3TOpGJAFblTikrs5KvEMshdFrhHiEzKAKd1vdt9ciDoQxmoLMRdJPJAD5eK7h%2F%2B%2B1R6C0ZtVlOuoOkzm17jcOsHgBqRqec5nVGyduMRyooT3PFlwbPTnnNG7D5uHfFzUeRLdodP%2Fk6tvecfOOgKZ2aM6O6uDzKRdbABS%2Fg9gfc2epxsNrT92BWdD5TSPqzKnFhdc0nfBxHFN0%2BnOh4on9V0FgteoZPdyMq3%2B9lUidLhMChw0Uy9ypTPE6JybXCT4eQR9A%2Fh7eDwojsVwThyGl0pzInT4KrKs9pj%2FabMlLzF%2B2g9H62fwzvW09taB6zS5tXPovRckV%2B5iefVS3A3he8svr6GxsA%2F%2FgD6DzoUDyvg1iXsr5bM4no4ybhaE8Rfd9HTShNsz9s1cF8WXqX1hnvwGXkTLIPCTpSkKdzJnNDP8n4o4UOiwW2W8bAkvWnPnjSVZnLp%2BFN7V8ydpVmXe51QdLPYrzf04Ywn2vD9QxCnTGcRdHFeyHyRWzds2BqN0UGvGqsfU94qiwPEvFKVJSAq1UdZiadp2sbbnHyLZedXVBFmzGTt2BjBXEHi6tXRtcUQ36jYLxy2NdR%2FhS33y1KoVRDaFMsnZuD6Of%2FdCju%2Ba2UBjIrTzOt7OfE1sqyI6RuLO7NzHgbpbmDknIvp%2BhHlSCcy9tFi%2FPsou7et7NU%2BQ%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20220523T123519Z&X-Amz-SignedHeaders=host&X-Amz-Expires=7200&X-Amz-Credential=ASIAWX7P4S4D4AE5K55E%2F20220523%2Feu-central-1%2Fs3%2Faws4_request&X-Amz-Signature=ff3d75125dad200d652c11f358ecf2c834b9ecc61b3e96e78ccf91805bf6062b")
                .build();

        var validRequest = createRequest(TYPE).destinationDataAddress(dataAddress).build();

        var sink = factory.createSink(validRequest);

        var result = sink.transfer(new InputStreamDataSource("test", new ByteArrayInputStream("test".getBytes()))).get();

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
        factory = new HttpDataSinkFactory(httpClient, Executors.newFixedThreadPool(1), 5, mock(Monitor.class));
    }


}

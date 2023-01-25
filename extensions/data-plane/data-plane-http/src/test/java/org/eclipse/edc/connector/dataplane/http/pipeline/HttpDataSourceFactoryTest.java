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
 *       Amadeus - test retrieval of auth code from vault
 *       Amadeus - add test for mapping of path segments
 *       Mercedes Benz Tech Innovation - add toggles for proxy behavior
 *
 */

package org.eclipse.edc.connector.dataplane.http.pipeline;

import org.eclipse.edc.connector.dataplane.http.params.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestParamsProvider;
import org.eclipse.edc.connector.dataplane.http.testfixtures.HttpTestFixtures;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.types.domain.HttpDataAddress.HTTP_DATA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpDataSourceFactoryTest {

    private final EdcHttpClient httpClient = mock(EdcHttpClient.class);
    private final Monitor monitor = mock(Monitor.class);
    private final HttpRequestParamsProvider provider = mock(HttpRequestParamsProvider.class);

    private HttpDataSourceFactory factory;

    @BeforeEach
    void setUp() {
        factory = new HttpDataSourceFactory(httpClient, provider, monitor);
    }

    @Test
    void verifyCanHandle() {
        assertThat(factory.canHandle(HttpTestFixtures.createRequest(HTTP_DATA).build())).isTrue();
    }

    @Test
    void verifyCannotHandle() {
        assertThat(factory.canHandle(HttpTestFixtures.createRequest("dummy").build())).isFalse();
    }

    @Test
    void verifyValidationFailsIfSupplierThrows() {
        var errorMsg = "Test error message";
        var request = createRequest(DataAddress.Builder.newInstance().type("Test type").build());

        when(provider.provideSourceParams(request)).thenThrow(new EdcException(errorMsg));

        var result = factory.validate(request);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).hasSize(1);
        assertThat(result.getFailureMessages().get(0)).contains(errorMsg);
    }

    @Test
    void verifySuccessSourceCreation() {
        var address = HttpDataAddress.Builder.newInstance()
                .name("test address name")
                .build();
        var request = createRequest(address);
        var params = mock(HttpRequestParams.class);

        when(provider.provideSourceParams(request)).thenReturn(params);

        assertThat(factory.validate(request).succeeded()).isTrue();
        var source = factory.createSource(request);
        assertThat(source).isNotNull();

        var expected = HttpDataSource.Builder.newInstance()
                .params(params)
                .name(address.getName())
                .requestId(request.getId())
                .httpClient(httpClient)
                .monitor(monitor)
                .build();

        assertThat(source).usingRecursiveComparison().isEqualTo(expected);
    }

    private DataFlowRequest createRequest(DataAddress source) {
        return DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source)
                .destinationDataAddress(DataAddress.Builder.newInstance().type("Test type").build())
                .build();
    }
}

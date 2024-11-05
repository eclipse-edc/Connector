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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities - improvements
 *
 */

package org.eclipse.edc.connector.dataplane.http.pipeline;

import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.connector.dataplane.http.testfixtures.TestFunctions;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpDataSourceFactoryTest {

    private final EdcHttpClient httpClient = mock();
    private final Monitor monitor = mock();
    private final HttpRequestParamsProvider provider = mock();
    private final HttpRequestFactory requestFactory = mock();

    private HttpDataSourceFactory factory;

    @BeforeEach
    void setUp() {
        factory = new HttpDataSourceFactory(httpClient, provider, monitor, requestFactory);
    }

    @Test
    void verifyValidationFailsIfSupplierThrows() {
        var errorMsg = "Test error message";
        var request = createRequest(DataAddress.Builder.newInstance().type("Test type").build());

        when(provider.provideSourceParams(request)).thenThrow(new EdcException(errorMsg));

        var result = factory.validateRequest(request);
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

        assertThat(factory.validateRequest(request).succeeded()).isTrue();
        var source = factory.createSource(request);
        assertThat(source).isNotNull();

        var expected = HttpDataSource.Builder.newInstance()
                .params(params)
                .name(address.getName())
                .requestId(request.getId())
                .httpClient(httpClient)
                .monitor(monitor)
                .requestFactory(requestFactory)
                .build();

        assertThat(source).usingRecursiveComparison().isEqualTo(expected);
    }

    private DataFlowStartMessage createRequest(DataAddress source) {
        return TestFunctions.createRequest(emptyMap(), source, DataAddress.Builder.newInstance().type("Test type").build()).build();
    }
}

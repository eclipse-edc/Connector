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

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import com.github.javafaker.Faker;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress.DATA_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpDataSourceFactoryTest {

    private static final Faker FAKER = new Faker();
    private static final OkHttpClient HTTP_CLIENT = mock(OkHttpClient.class);
    private static final RetryPolicy<Object> RETRY_POLICY = RetryPolicy.ofDefaults();

    private final HttpRequestParamsSupplier supplierMock = mock(HttpRequestParamsSupplier.class);

    private HttpDataSourceFactory factory;

    @BeforeEach
    void setUp() {
        factory = new HttpDataSourceFactory(HTTP_CLIENT, RETRY_POLICY, supplierMock);
    }

    @Test
    void verifyCanHandle() {
        assertThat(factory.canHandle(HttpTestFixtures.createRequest(DATA_TYPE).build())).isTrue();
    }

    @Test
    void verifyCannotHandle() {
        assertThat(factory.canHandle(HttpTestFixtures.createRequest("dummy").build())).isFalse();
    }

    @Test
    void verifyValidationFailsIfSupplierThrows() {
        var errorMsg = FAKER.lorem().sentence();
        var request = createRequest(DataAddress.Builder.newInstance().type(FAKER.lorem().word()).build());

        when(supplierMock.apply(request)).thenThrow(new EdcException(errorMsg));

        var result = factory.validate(request);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).hasSize(1);
        assertThat(result.getFailureMessages().get(0)).contains(errorMsg);
    }

    @Test
    void verifySuccessSourceCreation() {
        var address = HttpDataAddress.Builder.newInstance()
                .name(FAKER.lorem().word())
                .build();
        var request = createRequest(address);
        var params = mock(HttpRequestParams.class);

        when(supplierMock.apply(request)).thenReturn(params);

        assertThat(factory.validate(request).succeeded()).isTrue();
        var source = factory.createSource(request);
        assertThat(source).isNotNull();

        var expected = HttpDataSource.Builder.newInstance()
                .params(params)
                .name(address.getName())
                .requestId(request.getId())
                .retryPolicy(RETRY_POLICY)
                .httpClient(HTTP_CLIENT)
                .build();

        // validate the generated data source field by field using reflection
        Arrays.stream(HttpDataSource.class.getDeclaredFields()).forEach(f -> {
            f.setAccessible(true);
            try {
                assertThat(f.get(source)).isEqualTo(f.get(expected));
            } catch (IllegalAccessException e) {
                throw new AssertionError("Comparison failed for field: " + f.getName());
            }
        });
    }

    private static DataFlowRequest createRequest(DataAddress source) {
        return DataFlowRequest.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .processId(FAKER.internet().uuid())
                .sourceDataAddress(source)
                .destinationDataAddress(DataAddress.Builder.newInstance().type(FAKER.lorem().word()).build())
                .build();
    }
}

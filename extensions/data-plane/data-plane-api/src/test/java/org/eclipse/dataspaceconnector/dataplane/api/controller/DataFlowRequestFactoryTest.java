/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.api.controller;

import com.github.javafaker.Faker;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFlowRequestFactoryTest {

    private static final Faker FAKER = new Faker();

    @Test
    void from_requestWithoutBody() {
        var contextApi = mock(ContainerRequestContextApi.class);
        var address = createDataAddress();

        var method = HttpMethod.GET;
        var queryParams = FAKER.lorem().word();
        var path = FAKER.lorem().word();

        when(contextApi.method()).thenReturn(method);
        when(contextApi.queryParams()).thenReturn(queryParams);
        when(contextApi.path()).thenReturn(path);

        var request = DataFlowRequestFactory.from(contextApi, address);

        assertThat(request.isTrackable()).isFalse();
        assertThat(request.getId()).isNotBlank();
        assertThat(request.getDestinationDataAddress().getType()).isEqualTo(OutputStreamDataSinkFactory.TYPE);
        assertThat(request.getSourceDataAddress().getType()).isEqualTo(address.getType());
        assertThat(request.getProperties()).containsExactlyInAnyOrderEntriesOf(Map.of(
                DataFlowRequestSchema.PATH, path,
                DataFlowRequestSchema.METHOD, method,
                DataFlowRequestSchema.QUERY_PARAMS, queryParams

        ));
    }

    @Test
    void from_requestWithBody() {
        var contextApi = mock(ContainerRequestContextApi.class);
        var address = createDataAddress();

        var method = HttpMethod.GET;
        var queryParams = FAKER.lorem().word();
        var path = FAKER.lorem().word();
        var body = FAKER.lorem().sentence();

        when(contextApi.method()).thenReturn(method);
        when(contextApi.queryParams()).thenReturn(queryParams);
        when(contextApi.path()).thenReturn(path);
        when(contextApi.mediaType()).thenReturn(MediaType.TEXT_PLAIN);
        when(contextApi.body()).thenReturn(body);

        var request = DataFlowRequestFactory.from(contextApi, address);

        assertThat(request.isTrackable()).isFalse();
        assertThat(request.getId()).isNotBlank();
        assertThat(request.getDestinationDataAddress().getType()).isEqualTo(OutputStreamDataSinkFactory.TYPE);
        assertThat(request.getSourceDataAddress().getType()).isEqualTo(address.getType());
        assertThat(request.getProperties()).containsExactlyInAnyOrderEntriesOf(Map.of(
                DataFlowRequestSchema.PATH, path,
                DataFlowRequestSchema.METHOD, method,
                DataFlowRequestSchema.QUERY_PARAMS, queryParams,
                DataFlowRequestSchema.BODY, body,
                DataFlowRequestSchema.MEDIA_TYPE, MediaType.TEXT_PLAIN
        ));
    }

    private static DataAddress createDataAddress() {
        return DataAddress.Builder.newInstance().type(FAKER.lorem().word()).build();
    }
}
/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.http.api.decorator;

import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenSerDes;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogPaginationResponseDecoratorTest {

    private final String requestUrl = "http://request.url/path";
    private final ContinuationTokenSerDes continuationTokenSerDes = mock();
    private final Monitor monitor = mock();
    private final CatalogPaginationResponseDecorator decorator = new CatalogPaginationResponseDecorator(requestUrl, continuationTokenSerDes, monitor);

    @Nested
    class Next {
        @Test
        void shouldSetLink_whenDatasetCountEqualsToLimit() {
            var responseBuilder = Response.ok();
            var querySpec = QuerySpec.Builder.newInstance().offset(0).limit(1).build();
            var message = CatalogRequestMessage.Builder.newInstance().querySpec(querySpec).build();
            var catalog = Catalog.Builder.newInstance().dataset(Dataset.Builder.newInstance().build()).build();
            when(continuationTokenSerDes.serialize(any())).thenReturn(Result.success("serializedToken"));

            var response = decorator.decorate(responseBuilder, message, catalog).build();

            assertThat(response.hasLink("next")).isTrue();
            assertThat(response.getLink("next").getUri().toString()).isEqualTo(requestUrl + "?continuationToken=serializedToken");
            verify(continuationTokenSerDes).serialize(argThat(q -> q.getOffset() == 1));
        }

        @Test
        void shouldNotSetLink_whenDatasetSizeSmallerThanLimit() {
            var responseBuilder = Response.ok();
            var querySpec = QuerySpec.Builder.newInstance().limit(2).build();
            var message = CatalogRequestMessage.Builder.newInstance().querySpec(querySpec).build();
            var catalog = Catalog.Builder.newInstance().dataset(Dataset.Builder.newInstance().build()).build();

            var response = decorator.decorate(responseBuilder, message, catalog).build();

            assertThat(response.hasLink("next")).isFalse();
        }

        @Test
        void shouldNotSetLink_whenSerializationFails() {
            var responseBuilder = Response.ok();
            var querySpec = QuerySpec.Builder.newInstance().offset(0).limit(1).build();
            var message = CatalogRequestMessage.Builder.newInstance().querySpec(querySpec).build();
            var catalog = Catalog.Builder.newInstance().dataset(Dataset.Builder.newInstance().build()).build();
            when(continuationTokenSerDes.serialize(any())).thenReturn(Result.failure("error"));

            var response = decorator.decorate(responseBuilder, message, catalog).build();

            assertThat(response.hasLink("next")).isFalse();
            verify(monitor).warning(any(String.class));
        }
    }

    @Nested
    class Prev {
        @Test
        void shouldSetLink_whenOffsetGreaterOrEqualLimit() {
            var responseBuilder = Response.ok();
            var querySpec = QuerySpec.Builder.newInstance().offset(1).limit(1).build();
            var message = CatalogRequestMessage.Builder.newInstance().querySpec(querySpec).build();
            var catalog = Catalog.Builder.newInstance().build();
            when(continuationTokenSerDes.serialize(any())).thenReturn(Result.success("serializedToken"));

            var response = decorator.decorate(responseBuilder, message, catalog).build();

            assertThat(response.hasLink("prev")).isTrue();
            assertThat(response.getLink("prev").getUri().toString()).isEqualTo(requestUrl + "?continuationToken=serializedToken");
            verify(continuationTokenSerDes).serialize(argThat(q -> q.getOffset() == 0));
        }

        @Test
        void shouldNotSetLink_whenSerializationFails() {
            var responseBuilder = Response.ok();
            var querySpec = QuerySpec.Builder.newInstance().offset(1).limit(1).build();
            var message = CatalogRequestMessage.Builder.newInstance().querySpec(querySpec).build();
            var catalog = Catalog.Builder.newInstance().build();
            when(continuationTokenSerDes.serialize(any())).thenReturn(Result.failure("error"));

            var response = decorator.decorate(responseBuilder, message, catalog).build();

            assertThat(response.hasLink("next")).isFalse();
            verify(monitor).warning(any(String.class));
        }
    }

}

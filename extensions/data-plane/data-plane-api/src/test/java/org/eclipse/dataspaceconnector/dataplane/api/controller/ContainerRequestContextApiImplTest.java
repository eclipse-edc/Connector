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
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.PATH;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerRequestContextApiImplTest {

    private static final Faker FAKER = new Faker();

    private final ContainerRequestContextApi api = new ContainerRequestContextApiImpl();

    @Test
    void authHeader() {
        var token = FAKER.internet().uuid();
        var context = mock(ContainerRequestContext.class);
        when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);

        assertThat(api.authHeader(context)).isEqualTo(token);
    }

    @Test
    void properties() {
        var body = FAKER.internet().uuid();
        var headers = defaultHeaders();
        var path = FAKER.internet().uuid();
        var context = testContext(testQueryParams(), path, headers, MediaType.valueOf(APPLICATION_JSON), body);

        var props = api.properties(context);

        assertThat(props).containsExactlyInAnyOrderEntriesOf(Map.of(
                METHOD, HttpMethod.POST,
                QUERY_PARAMS, "foo=bar&hello=world",
                MEDIA_TYPE, APPLICATION_JSON,
                PATH, path,
                BODY, body
        ));
    }

    private ContainerRequestContext testContext(MultivaluedMap<String, String> queryParams,
                                                String path,
                                                MultivaluedMap<String, String> headers,
                                                @Nullable MediaType mediaType,
                                                @Nullable String body) {
        var uriInfo = createMockUriInfo(queryParams, path);

        var context = mock(ContainerRequestContext.class);
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(context.getMethod()).thenReturn(HttpMethod.POST);
        when(context.getHeaders()).thenReturn(headers);

        if (mediaType != null && body != null) {
            when(context.hasEntity()).thenReturn(true);
            when(context.getMediaType()).thenReturn(mediaType);
            when(context.getEntityStream()).thenReturn(new ByteArrayInputStream(body.getBytes()));
        }

        return context;
    }

    private static UriInfo createMockUriInfo(MultivaluedMap<String, String> queryParams, String path) {
        var uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);
        when(uriInfo.getPath()).thenReturn(path);
        when(uriInfo.getBaseUri()).thenReturn(URI.create(FAKER.internet().url()));
        return uriInfo;
    }

    private static MultivaluedMap<String, String> defaultHeaders() {
        var headers = new MultivaluedHashMap<String, String>();
        headers.put("Authorization", List.of(FAKER.internet().uuid()));
        return headers;
    }

    private MultivaluedMap<String, String> testQueryParams() {
        var queryParams = new MultivaluedHashMap<String, String>();
        queryParams.put("foo", Arrays.asList("bar", "hey"));
        queryParams.put("hello", List.of("world"));
        return queryParams;
    }
}
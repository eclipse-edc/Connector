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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.PATH;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;

/**
 * This class provides a set of API wrapping a {@link ContainerRequestContext}.
 */
public class ContainerRequestContextApiImpl implements ContainerRequestContextApi {

    @Override
    public @Nullable String authHeader(ContainerRequestContext context) {
        return context.getHeaderString(HttpHeaders.AUTHORIZATION);
    }

    @Override
    public Map<String, String> properties(ContainerRequestContext context) {
        var props = new HashMap<String, String>();
        props.put(METHOD, context.getMethod());
        props.put(QUERY_PARAMS, convertQueryParamsToString(context.getUriInfo()));

        var pathInfo = context.getUriInfo().getPath();
        if (!pathInfo.isBlank()) {
            props.put(PATH, pathInfo);
        }

        var mediaType = context.getMediaType();
        Optional.ofNullable(mediaType).ifPresent(mt -> {
            props.put(MEDIA_TYPE, mediaType.toString());
            props.put(BODY, readRequestBody(context));
        });
        return props;
    }

    private static String readRequestBody(ContainerRequestContext requestContext) {
        // TODO: limit the maximum amount of bytes read from the channel.
        try (BufferedReader br = new BufferedReader(new InputStreamReader(requestContext.getEntityStream()))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new EdcException("Failed to read request body: " + e.getMessage());
        }
    }

    /**
     * Convert query parameters injected from the input into a string, e.g. foo=bar&hello=world
     */
    private static String convertQueryParamsToString(UriInfo uriInfo) {
        return uriInfo.getQueryParameters().entrySet()
                .stream()
                .map(entry -> new QueryParam(entry.getKey(), entry.getValue()))
                .filter(QueryParam::isValid)
                .map(QueryParam::toString)
                .collect(Collectors.joining("&"));
    }

    private static final class QueryParam {

        private final String key;
        private final List<String> values;
        private final boolean valid;

        private QueryParam(String key, List<String> values) {
            this.key = key;
            this.values = values;
            this.valid = key != null && values != null && !values.isEmpty();
        }

        public boolean isValid() {
            return valid;
        }

        @Override
        public String toString() {
            return valid ? key + "=" + values.get(0) : "";
        }
    }
}

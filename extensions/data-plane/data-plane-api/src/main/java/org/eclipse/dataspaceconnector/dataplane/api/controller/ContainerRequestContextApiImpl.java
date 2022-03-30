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
 *       Daimler TSS GmbH - security improvement: don't overwrite values of DataAddress
 *
 */

package org.eclipse.dataspaceconnector.dataplane.api.controller;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides a set of API wrapping a {@link ContainerRequestContext}.
 */
public class ContainerRequestContextApiImpl implements ContainerRequestContextApi {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public @Nullable String authHeader(ContainerRequestContext context) {
        return context.getHeaderString(AUTHORIZATION_HEADER);
    }

    @Override
    public String queryParams(ContainerRequestContext context) {
        return context.getUriInfo().getQueryParameters().entrySet()
                .stream()
                .map(entry -> new QueryParam(entry.getKey(), entry.getValue()))
                .filter(QueryParam::isValid)
                .map(QueryParam::toString)
                .collect(Collectors.joining("&"));
    }

    @Override
    public String body(ContainerRequestContext context) {
        // TODO: limit the maximum amount of bytes read from the channel.
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getEntityStream()))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new EdcException("Failed to read request body: " + e.getMessage());
        }
    }

    @Override
    public String path(ContainerRequestContext context) {
        var pathInfo = context.getUriInfo().getPath();
        if (!pathInfo.isBlank()) {
            return pathInfo;
        }
        return null;
    }

    @Override
    public String mediaType(ContainerRequestContext context) {
        var mediaType = context.getMediaType();
        if (mediaType != null) {
            return mediaType.toString();
        }
        return null;
    }

    @Override
    public String method(ContainerRequestContext context) {
        return context.getMethod();
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

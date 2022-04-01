/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.extension.jersey;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

public class CorsFilter implements ContainerResponseFilter {

    private final CorsFilterConfiguration config;

    public CorsFilter(CorsFilterConfiguration config) {
        this.config = config;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().add("Access-Control-Allow-Origin", config.getAllowedOrigins());
        responseContext.getHeaders().add("Access-Control-Allow-Headers", config.getAllowedHeaders());
        responseContext.getHeaders().add("Access-Control-Allow-Methods", config.getAllowedMethods());

    }
}

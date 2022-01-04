package org.eclipse.dataspaceconnector.core.protocol.web.rest;

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

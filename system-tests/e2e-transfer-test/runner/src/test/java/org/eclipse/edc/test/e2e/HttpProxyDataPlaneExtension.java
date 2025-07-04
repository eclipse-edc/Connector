/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static java.util.Collections.emptyMap;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Extension that provides a dummy proxy that always return a hardcoded successful response when the token validation
 * succeeds.
 */
public class HttpProxyDataPlaneExtension implements ServiceExtension {

    private static final String API_CONTEXT = "proxy";

    @Inject
    private DataPlaneAuthorizationService authorizationService;
    @Inject
    private PublicEndpointGeneratorService generatorService;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private WebService webService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var portMapping = new PortMapping(API_CONTEXT, getFreePort(), "/proxy");
        portMappingRegistry.register(portMapping);

        var proxyUrl = "http://localhost:%d%s".formatted(portMapping.port(), portMapping.path());
        generatorService.addGeneratorFunction("HttpData", address -> Endpoint.url(proxyUrl));

        generatorService.addGeneratorFunction("HttpData", () -> Endpoint.url(proxyUrl));

        webService.registerResource(API_CONTEXT, new Controller(authorizationService));
    }

    @Path("{any:.*}")
    @Consumes(WILDCARD)
    @Produces(WILDCARD)
    public static class Controller {

        private final DataPlaneAuthorizationService authorizationService;

        Controller(DataPlaneAuthorizationService authorizationService) {
            this.authorizationService = authorizationService;
        }

        @GET
        public Response get(@Context ContainerRequestContext requestContext) {
            var token = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null) {
                return Response.status(UNAUTHORIZED).build();
            }

            var sourceDataAddress = authorizationService.authorize(token, emptyMap());
            if (sourceDataAddress.failed()) {
                return Response.status(FORBIDDEN).build();
            }

            return Response.ok("data").build();
        }

        @POST
        public Response postResponse(@Context ContainerRequestContext requestContext) {
            var token = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null) {
                return Response.status(UNAUTHORIZED).build();
            }

            var sourceDataAddress = authorizationService.authorize(token, emptyMap());
            if (sourceDataAddress.failed()) {
                return Response.status(FORBIDDEN).build();
            }

            var dataAddressUrl = sourceDataAddress.getContent().getStringProperty(EDC_NAMESPACE + "baseUrl");

            if (dataAddressUrl != null && !dataAddressUrl.equals("http://any/response/channel")) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            }

            return Response.ok("response received").build();
        }
    }
}

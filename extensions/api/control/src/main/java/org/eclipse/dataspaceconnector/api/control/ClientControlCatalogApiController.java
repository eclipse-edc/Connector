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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.control;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Deprecated
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/control")
public class ClientControlCatalogApiController implements ClientControlCatalogApi {

    private final RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry;

    public ClientControlCatalogApiController(@NotNull RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry) {
        this.remoteMessageDispatcherRegistry = Objects.requireNonNull(remoteMessageDispatcherRegistry, "remoteMessageDispatcherRegistry");
    }

    @GET
    @Path("/catalog")
    @Override
    public void getDescription(@QueryParam("provider") String provider, @Suspended AsyncResponse asyncResponse) {
        if (provider == null) {
            asyncResponse.resume(badRequest("provider required"));
            return;
        }

        CatalogRequest metadataRequest = CatalogRequest.Builder.newInstance()
                .protocol("ids-multipart")
                .connectorAddress(provider)
                .connectorId(provider)
                .build();

        CompletableFuture<Object> future = remoteMessageDispatcherRegistry
                .send(Object.class, metadataRequest, () -> null);

        future.whenComplete(fillResponse(asyncResponse));
    }

    private static <T> BiConsumer<T, Throwable> fillResponse(AsyncResponse asyncResponse) {
        return (result, error) -> {
            if (error != null) {
                asyncResponse.resume(serverError(error.getMessage()));
            } else {
                asyncResponse.resume(ok(result));
            }
        };
    }

    private static Response ok(Object entity) {
        Response.ResponseBuilder builder = Response.ok();
        if (entity != null) {
            builder.entity(entity);
        }
        return builder.build();
    }

    private static Response serverError(Object entity) {
        Response.ResponseBuilder builder = Response.serverError();
        if (entity != null) {
            builder.entity(entity);
        }
        return builder.build();
    }

    private static Response badRequest(Object entity) {
        Response.ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST);
        if (entity != null) {
            builder.entity(entity);
        }
        return builder.build();
    }
}

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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Path("/consumer")
public class ConsumerBackendServiceController {

    private final Monitor monitor;
    private final AtomicReference<String> data = new AtomicReference<>();
    private final Map<String, EndpointDataReference> dataReference = new ConcurrentHashMap<>();

    public ConsumerBackendServiceController(Monitor monitor) {
        this.monitor = monitor;
    }

    @Path("/dataReference")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public void pushDataReference(EndpointDataReference edr) {
        monitor.debug("Received new endpoint data reference with url " + edr.getEndpoint());
        dataReference.put(edr.getId(), edr);
    }

    @Path("/dataReference/{id}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public EndpointDataReference getDataReference(@PathParam("id") String id) {
        return Optional.ofNullable(dataReference.get(id)).orElseThrow(NoSuchElementException::new);
    }

    @Path("/store")
    @POST
    public void pushData(String body) {
        data.set(body);
    }

    @Path("/data")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getData() {
        return data.get();
    }

}

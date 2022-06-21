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

package org.eclipse.dataspaceconnector.test.e2e;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

@Path("/consumer")
public class ConsumerBackendServiceController {

    private final Monitor monitor;
    private final OkHttpClient httpClient;
    private final AtomicReference<String> data = new AtomicReference<>();

    public ConsumerBackendServiceController(Monitor monitor, OkHttpClient httpClient) {
        this.monitor = monitor;
        this.httpClient = httpClient;
    }

    @Path("/pull")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public void pullData(EndpointDataReference dataReference) {
        String url = dataReference.getEndpoint();
        monitor.debug("Endpoint Data Reference received, will call data plane at " + url);
        var request = new Request.Builder()
                .url(url)
                .addHeader(dataReference.getAuthKey(), dataReference.getAuthCode())
                .build();

        try (var response = httpClient.newCall(request).execute()) {
            var body = response.body();
            var string = body.string();
            if (response.isSuccessful()) {
                monitor.info("Data plane responded correctly: " + string);
                data.set(string);
            } else {
                monitor.warning(format("Data plane responded with error: %s %s", response.code(), string));
            }
        } catch (Exception e) {
            monitor.severe(format("Error in calling the data plane at %s", url), e);
        }
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

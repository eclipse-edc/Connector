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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Map;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static okhttp3.MediaType.get;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@Path("/provision")
public class BackendServiceHttpProvisionerController {

    private final Monitor monitor;
    private final EdcHttpClient httpClient;
    private final TypeManager typeManager;
    private final int exposedHttpPort;

    public BackendServiceHttpProvisionerController(Monitor monitor, EdcHttpClient httpClient, TypeManager typeManager, int exposedHttpPort) {
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.typeManager = typeManager;
        this.exposedHttpPort = exposedHttpPort;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void provision(Map<String, Object> request) {
        var baseUrl = request.get("callbackAddress");
        var transferProcessId = request.get("transferProcessId");
        var completeUrl = format("%s/%s/provision", baseUrl, transferProcessId);
        monitor.info(format("Provision request received. Will now call the callback address %s to fake the provisioning", completeUrl));
        var requestBody = Map.of(
                "edctype", "dataspaceconnector:provisioner-callback-request",
                "resourceDefinitionId", request.get("resourceDefinitionId"),
                "assetId", request.get("assetId"),
                "resourceName", "aName",
                // this is the data address of the content served by this backend service
                "contentDataAddress", Map.of(
                        "properties", Map.of(
                                EDC_NAMESPACE + "type", "HttpData",
                                EDC_NAMESPACE + "baseUrl", format("http://localhost:%d/api/provider/data", exposedHttpPort)
                        )
                ),
                "apiKeyJwt", "unused",
                "hasToken", false
        );

        Executors.newScheduledThreadPool(1).schedule(() -> callbackRequest(completeUrl, requestBody), 5, SECONDS);
    }

    private void callbackRequest(String completeUrl, Map<String, Object> requestBody) {
        var callbackRequest = new Request.Builder()
                .url(completeUrl)
                .post(RequestBody.create(typeManager.writeValueAsString(requestBody), get("application/json")))
                .build();

        try (var response = httpClient.execute(callbackRequest)) {
            var body = response.body();
            var string = body.string();
            if (response.isSuccessful()) {
                monitor.info("Provisioning callback responded correctly: " + string);

            } else {
                monitor.warning(format("Provisioning callback responded with error: %s %s", response.code(), string));
            }
        } catch (Exception e) {
            monitor.severe(format("Error in calling the provisioning callback at %s", completeUrl), e);
        }
    }
}

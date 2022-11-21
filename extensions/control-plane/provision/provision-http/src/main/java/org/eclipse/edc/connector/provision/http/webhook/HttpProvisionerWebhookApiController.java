/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.provision.http.webhook;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.provision.http.impl.HttpProvisionedContentResource;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.UUID;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/callback")
public class HttpProvisionerWebhookApiController implements HttpProvisionerWebhookApi {
    private final TransferProcessService transferProcessService;

    public HttpProvisionerWebhookApiController(TransferProcessService transferProcessService) {
        this.transferProcessService = transferProcessService;
    }

    @Override
    @POST
    @Path("/{processId}/provision")
    public void callProvisionWebhook(@PathParam("processId") String transferProcessId, @Valid ProvisionerWebhookRequest request) {
        var contentResource = HttpProvisionedContentResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .assetId(request.getAssetId())
                .dataAddress(request.getContentDataAddress())
                .resourceName(request.getResourceName())
                .transferProcessId(transferProcessId)
                .hasToken(request.hasToken())
                .resourceDefinitionId(request.getResourceDefinitionId())
                .build();

        var response = ProvisionResponse.Builder.newInstance()
                .resource(contentResource)
                .secretToken(new SimpleSecretToken(request.getApiKeyJwt()))
                .build();

        transferProcessService.addProvisionedResource(transferProcessId, response)
                .orElseThrow(exceptionMapper(TransferProcess.class, transferProcessId));

    }

    @Override
    @POST
    @Path("/{processId}/deprovision")
    public void callDeprovisionWebhook(@PathParam("processId") String transferProcessId, @Valid DeprovisionedResource resource) {
        if (resource == null || resource.getProvisionedResourceId() == null) {
            throw new InvalidRequestException("Request body cannot be null and it should provide a valid provisionedResourceId value");
        }

        transferProcessService.completeDeprovision(transferProcessId, resource)
                .orElseThrow(exceptionMapper(TransferProcess.class, transferProcessId));
    }

}

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

package org.eclipse.dataspaceconnector.transfer.provision.http.webhook;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.spi.exception.InvalidRequestException;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.AddProvisionedResourceCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.DeprovisionCompleteCommand;
import org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProvisionedContentResource;

import java.util.UUID;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/callback")
public class HttpProvisionerWebhookApiController implements HttpProvisionerWebhookApi {
    private final TransferProcessManager transferProcessManager;

    public HttpProvisionerWebhookApiController(TransferProcessManager transferProcessManager) {
        this.transferProcessManager = transferProcessManager;
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

        var command = new AddProvisionedResourceCommand(transferProcessId, response);

        transferProcessManager.enqueueCommand(command);

    }

    @Override
    @POST
    @Path("/{processId}/deprovision")
    public void callDeprovisionWebhook(@PathParam("processId") String transferProcessId, @Valid DeprovisionedResource resource) {
        if (resource == null || resource.getProvisionedResourceId() == null) {
            throw new InvalidRequestException("Request body cannot be null and it should provide a valid provisionedResourceId value");
        }

        var command = new DeprovisionCompleteCommand(transferProcessId, resource);

        transferProcessManager.enqueueCommand(command);
    }

}

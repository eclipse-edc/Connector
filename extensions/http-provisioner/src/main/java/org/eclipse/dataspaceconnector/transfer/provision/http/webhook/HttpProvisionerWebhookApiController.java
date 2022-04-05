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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.AddProvisionedResourceCommand;
import org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProvisionedContentResource;

import java.util.Objects;
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
    public void callWebhook(ProvisionerWebhookRequest request) {

        Objects.requireNonNull(request);
        Objects.requireNonNull(request.getAssetId());
        Objects.requireNonNull(request.getContentDataAddress());
        Objects.requireNonNull(request.getResourceName());
        Objects.requireNonNull(request.getTransferProcessId());
        Objects.requireNonNull(request.getResourceDefinitionId());

        var cr = HttpProvisionedContentResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .assetId(request.getAssetId())
                .dataAddress(request.getContentDataAddress())
                .resourceName(request.getResourceName())
                .transferProcessId(request.getTransferProcessId())
                .hasToken(request.hasToken())
                .resourceDefinitionId(request.getResourceDefinitionId())
                .build();

        var response = ProvisionResponse.Builder.newInstance()
                .resource(cr)
                .secretToken(new SimpleSecretToken(request.getApiToken()))
                .build();
        var cmd = new AddProvisionedResourceCommand(request.getTransferProcessId(), response);

        transferProcessManager.enqueueCommand(cmd);

    }

}

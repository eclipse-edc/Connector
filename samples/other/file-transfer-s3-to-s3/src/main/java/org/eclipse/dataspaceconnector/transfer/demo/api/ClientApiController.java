/*
 *  Copyright (c) 2021 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Siemens AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.util.Map;
import java.util.UUID;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/transfer")
public class ClientApiController {

    private static final String PREFIX = "http://";
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;
    private final TransferProcessManager processManager;
    private final Monitor monitor;
    private final String destinationRegion;
    private final String destinationBucket;

    public ClientApiController(
            RemoteMessageDispatcherRegistry dispatcherRegistry,
            TransferProcessManager processManager,
            Monitor monitor,
            String destinationRegion, String destinationBucket) {
        this.dispatcherRegistry = dispatcherRegistry;
        this.processManager = processManager;
        this.monitor = monitor;
        this.destinationRegion = destinationRegion;
        this.destinationBucket = destinationBucket;
    }

    @POST
    @Path("data/request")
    public Response initiateDataRequest(Map<String, String> request) {
        var connector = (String) request.get("connector");
        var artifact = (String) request.get("artifact");

        var destinationName = extractDestinationArtifactName(artifact);

        var usRequest = createRequest(
                connector,
                UUID.randomUUID().toString(),
                Asset.Builder.newInstance().id(artifact).build(),
                destinationBucket,
                destinationName,
                destinationRegion);

        processManager.initiateConsumerRequest(usRequest);
        return Response.ok().build();
    }

    private String extractDestinationArtifactName(String artifactId) {
        return artifactId.substring(artifactId.lastIndexOf('/') + 1);
    }

    private DataRequest createRequest(
            String connector,
            String id,
            Asset asset,
            String destinationBucket,
            String destinationName,
            String destinationRegion
    ) {

        // For now this will always throw an exception because IDS REST is not supported.
        // The strange 'if' is necessary, because otherwise we'd have to delete all the code after the exception to avoid
        // compiler errors ("unreachable code"), which I didn't want to do.
        // TODO: either support IDS Multipart or delete this module.

        if (1 == 1) {
            throw new NotSupportedException("IDS REST is not supported anymore. Please adapt the code to use IDS Multipart!");
        }

        return DataRequest.Builder.newInstance()
                .id(id)
                //.protocol(Protocols.IDS_REST) //change to Protocols.IDS_MULTIPART
                .assetId(asset.getId())
                .connectorId(connector)
                .connectorAddress(composeConnectorAddress(connector))
                .dataDestination(DataAddress.Builder.newInstance()
                        .keyName(destinationName)
                        .property("bucketName", destinationBucket)
                        .property("region", destinationRegion)
                        .type("dataspaceconnector:s3")
                        .build())
                .build();
    }

    private String composeConnectorAddress(String connector) {
        return PREFIX + connector;
    }
}

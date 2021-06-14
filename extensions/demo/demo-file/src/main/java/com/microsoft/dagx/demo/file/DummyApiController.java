/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.demo.file;

import java.util.UUID;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;

import com.microsoft.dagx.demo.file.zip.schema.ZipSchema;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/files")
public class DummyApiController {

    private final TransferProcessManager processManager;
    private final Monitor monitor;

    public DummyApiController(TransferProcessManager processManager, Monitor monitor) {
        this.processManager = processManager;
        this.monitor = monitor;
    }

    // Request an artifact. E.g. to request 'file1' and store it in archive.zip
    // http://localhost:8181/api/files/file1?zip=archive
    @GET
    @Path("{artifactId}")
    public Response requestArtifact(@Context UriInfo uri,
                                    @PathParam("artifactId") String artifactId,
                                    @QueryParam("zip") String zipFileName) {

        monitor.info("Artifact request initialized");

        var entry = DataEntry.Builder.newInstance().id(artifactId).build();
        var connector = uri.getBaseUri().getHost() + ":" + uri.getBaseUri().getPort(); // connector should consume own request

        var destination = DataAddress.Builder.newInstance()
                .type(ZipSchema.TYPE)
                .property(ZipSchema.DIRECTORY, Configuration.TmpDirectory)
                .property(ZipSchema.NAME, zipFileName)
                .keyName("password")
                .build();

        var request = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("ids-rest")
                .dataEntry(entry)
                .dataDestination(destination)
                .destinationType("edc:zipArchive")
                .connectorId(connector)
                .connectorAddress("http://" + connector)
                .build();

        processManager.initiateClientRequest(request);
        return Response.accepted().build();
    }
}

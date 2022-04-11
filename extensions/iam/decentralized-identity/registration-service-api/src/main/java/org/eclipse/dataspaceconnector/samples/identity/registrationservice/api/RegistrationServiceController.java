/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.samples.identity.registrationservice.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.store.DidStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/identity")
public class RegistrationServiceController {
    private final Monitor monitor;
    private final DidStore didDocumentStore;

    public RegistrationServiceController(Monitor monitor, DidStore didDocumentStore) {
        this.monitor = monitor;
        this.didDocumentStore = didDocumentStore;
    }

    @GET
    @Path("/dids")
    public Response getDids(@QueryParam("since") String offset) {
        monitor.info("Fetching all DIDs");
        List<DidDocument> allDids;
        if (StringUtils.isNullOrBlank(offset)) {
            allDids = getAllDids(100);
        } else {
            allDids = getDidsWithOffset(offset);
        }

        return Response.ok(allDids).build();
    }

    @GET
    @Path("/dids/{did}")
    public DidDocument getDid(@PathParam("did") String did) {
        return didDocumentStore.forId(did);
    }

    private List<DidDocument> getDidsWithOffset(String offset) {
        return didDocumentStore.getAfter(offset);
    }

    private List<DidDocument> getAllDids(int maxNumber) {
        return didDocumentStore.getAll(maxNumber);
    }
}

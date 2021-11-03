/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.ids.api.catalog;

import de.fraunhofer.iais.eis.QueryMessage;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.ids.spi.daps.DapsService;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import static de.fraunhofer.iais.eis.RejectionReason.MALFORMED_MESSAGE;
import static de.fraunhofer.iais.eis.RejectionReason.NOT_AUTHENTICATED;
import static java.util.stream.Collectors.toList;

/**
 * Handles incoming consumer data catalog queries.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/ids")
public class CatalogQueryController {
    private final QueryEngine queryEngine;
    private final DapsService dapsService;

    public CatalogQueryController(QueryEngine queryEngine, DapsService dapsService) {
        this.queryEngine = queryEngine;
        this.dapsService = dapsService;
    }

    @POST
    @Path("query")
    public Response query(QueryMessage message) {
        var connectorId = message.getIssuerConnector().toString();

        var verificationResult = dapsService.verifyAndConvertToken(message.getSecurityToken().getTokenValue());
        if (!verificationResult.valid()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_AUTHENTICATED).build()).build();
        }

        var query = (String) message.getProperties().get("query");
        if (query == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(MALFORMED_MESSAGE).build()).build();
        }

        var language = (String) message.getProperties().get("queryLanguage");
        if (language == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(MALFORMED_MESSAGE).build()).build();
        }

        var correlationId = message.getId().toString();

        var consumerToken = verificationResult.token();

        var results = queryEngine.execute(correlationId, consumerToken, connectorId, language, query);
        return Response.ok(results.stream().map(Asset::getId).collect(toList())).build();
    }
}

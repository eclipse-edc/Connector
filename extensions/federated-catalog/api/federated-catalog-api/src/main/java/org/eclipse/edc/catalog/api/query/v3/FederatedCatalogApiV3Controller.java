/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.catalog.api.query.v3;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.edc.catalog.api.query.BaseFederatedCatalogApiController;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3/federatedcatalog")
public class FederatedCatalogApiV3Controller extends BaseFederatedCatalogApiController implements FederatedCatalogApiV3 {

    public FederatedCatalogApiV3Controller(QueryService queryService, TypeTransformerRegistry transformerRegistry) {
        super(queryService, transformerRegistry);
    }

    @Override
    @POST
    public JsonArray getCachedCatalogV3(JsonObject querySpecJson, @DefaultValue("false") @QueryParam("flatten") boolean flatten) {
        return getCachedCatalog(querySpecJson, flatten);
    }
}

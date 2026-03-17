/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.api.query.v4;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.edc.catalog.api.query.BaseCatalogsApiController;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4beta/catalogs")
public class CatalogsApiV4Controller extends BaseCatalogsApiController implements CatalogsApiV4 {

    public CatalogsApiV4Controller(QueryService queryService, TypeTransformerRegistry transformerRegistry) {
        super(queryService, transformerRegistry);
    }

    @Override
    @POST
    @Path("/request")
    public JsonArray requestCatalogsV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson, @DefaultValue("false") @QueryParam("flatten") boolean flatten) {
        return requestCatalogs(querySpecJson, flatten);
    }
}

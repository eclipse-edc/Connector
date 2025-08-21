/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.catalog.v4;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.edc.connector.controlplane.api.management.catalog.BaseCatalogApiController;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4alpha/catalog")
public class CatalogApiV4Controller extends BaseCatalogApiController implements CatalogApiV4 {
    public CatalogApiV4Controller(CatalogService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, validatorRegistry);
    }

    @POST
    @Path("/request")
    @Override
    public void requestCatalogV4(@SchemaType(CATALOG_REQUEST_TYPE_TERM) JsonObject request, @Suspended AsyncResponse response) {
        requestCatalog(request, response);
    }

    @POST
    @Path("dataset/request")
    @Override
    public void getDatasetV4(@SchemaType(DATASET_REQUEST_TYPE_TERM) JsonObject request, @Suspended AsyncResponse response) {
        getDataset(request, response);
    }
}

/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.catalog.v2;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.edc.connector.controlplane.api.management.catalog.BaseCatalogApiController;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/catalog")
public class CatalogApiV2Controller extends BaseCatalogApiController implements CatalogApiV2 {
    private final Monitor monitor;

    public CatalogApiV2Controller(CatalogService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry, Monitor monitor) {
        super(service, transformerRegistry, validatorRegistry);
        this.monitor = monitor;
    }

    @POST
    @Path("/request")
    @Override
    public void requestCatalogV2(JsonObject requestBody, @Suspended AsyncResponse response) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        requestCatalog(requestBody, response);
    }

    @POST
    @Path("dataset/request")
    @Override
    public void getDatasetV2(JsonObject requestBody, @Suspended AsyncResponse response) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        getDataset(requestBody, response);
    }
}

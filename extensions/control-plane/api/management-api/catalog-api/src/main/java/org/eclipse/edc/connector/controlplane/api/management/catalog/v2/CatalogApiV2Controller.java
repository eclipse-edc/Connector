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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import org.eclipse.edc.api.ApiWarnings;
import org.eclipse.edc.connector.controlplane.api.management.catalog.BaseCatalogApiController;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Path("/v2/catalog")
public class CatalogApiV2Controller extends BaseCatalogApiController implements CatalogApiV2 {
    private final Monitor monitor;

    public CatalogApiV2Controller(CatalogService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry, Monitor monitor) {
        super(service, transformerRegistry, validatorRegistry);
        this.monitor = monitor;
    }

    @Override
    public void requestCatalog(JsonObject requestBody, AsyncResponse response) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        super.requestCatalog(requestBody, response);
    }

    @Override
    public void getDataset(JsonObject requestBody, AsyncResponse response) {
        monitor.warning(ApiWarnings.deprecationWarning("/v2", "/v3"));
        super.getDataset(requestBody, response);
    }
}

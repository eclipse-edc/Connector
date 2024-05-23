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

package org.eclipse.edc.connector.controlplane.api.management.catalog.v3;

import jakarta.ws.rs.Path;
import org.eclipse.edc.connector.controlplane.api.management.catalog.BaseCatalogApiController;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Path("/v3/catalog")
public class CatalogApiV3Controller extends BaseCatalogApiController implements CatalogApiV3 {
    public CatalogApiV3Controller(CatalogService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, validatorRegistry);
    }
}

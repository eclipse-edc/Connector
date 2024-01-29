/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.api.management.catalog;

import org.eclipse.edc.connector.api.management.catalog.transform.JsonObjectToCatalogRequestTransformer;
import org.eclipse.edc.connector.api.management.catalog.transform.JsonObjectToDatasetRequestTransformer;
import org.eclipse.edc.connector.api.management.catalog.validation.CatalogRequestValidator;
import org.eclipse.edc.connector.api.management.catalog.validation.DatasetRequestValidator;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.configuration.transform.ManagementApiTypeTransformerRegistry;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.catalog.spi.DatasetRequest.DATASET_REQUEST_TYPE;

@Extension(value = CatalogApiExtension.NAME)
public class CatalogApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Catalog";

    @Inject
    private WebService webService;

    @Inject
    private ManagementApiConfiguration config;

    @Inject
    private ManagementApiTypeTransformerRegistry transformerRegistry;

    @Inject
    private CatalogService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new JsonObjectToCatalogRequestTransformer());
        transformerRegistry.register(new JsonObjectToDatasetRequestTransformer());

        webService.registerResource(config.getContextAlias(), new CatalogApiController(service, transformerRegistry, validatorRegistry));

        validatorRegistry.register(CATALOG_REQUEST_TYPE, CatalogRequestValidator.instance(context.getMonitor(), criterionOperatorRegistry));
        validatorRegistry.register(DATASET_REQUEST_TYPE, DatasetRequestValidator.instance());
    }
}

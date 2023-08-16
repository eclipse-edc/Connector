/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.api;

import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfiguration;
import org.eclipse.edc.protocol.dsp.catalog.api.controller.DspCatalogApiController;
import org.eclipse.edc.protocol.dsp.catalog.api.validation.CatalogRequestMessageValidator;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.protocol.dsp.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE;

/**
 * Creates and registers the controller for dataspace protocol catalog requests.
 */
@Extension(value = DspCatalogApiExtension.NAME)
public class DspCatalogApiExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Catalog Extension";

    @Inject
    private WebService webService;
    @Inject
    private IdentityService identityService;
    @Inject
    private DspApiConfiguration apiConfiguration;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private CatalogProtocolService service;
    @Inject
    private DataServiceRegistry dataServiceRegistry;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        validatorRegistry.register(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE, CatalogRequestMessageValidator.instance());
        var dspCallbackAddress = apiConfiguration.getDspCallbackAddress();
        var catalogController = new DspCatalogApiController(context.getMonitor(), identityService, transformerRegistry,
                dspCallbackAddress, service, validatorRegistry);
        webService.registerResource(apiConfiguration.getContextAlias(), catalogController);

        dataServiceRegistry.register(DataService.Builder.newInstance()
                .terms("connector")
                .endpointUrl(apiConfiguration.getDspCallbackAddress())
                .build());
    }
}

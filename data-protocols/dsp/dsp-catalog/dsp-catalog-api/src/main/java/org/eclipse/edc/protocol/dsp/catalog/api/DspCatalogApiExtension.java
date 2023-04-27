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
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfiguration;
import org.eclipse.edc.protocol.dsp.catalog.api.controller.CatalogController;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.jsonld.JsonLdExtension.TYPE_MANAGER_CONTEXT_JSON_LD;

/**
 * Creates and registers the controller for dataspace protocol catalog requests.
 */
@Extension(value = DspCatalogApiExtension.NAME)
public class DspCatalogApiExtension implements ServiceExtension {
    
    public static final String NAME = "Dataspace Protocol Catalog Extension";

    @Inject
    private WebService webService;
    @Inject
    private TypeManager typeManager;
    @Inject
    private IdentityService identityService;
    @Inject
    private DspApiConfiguration apiConfiguration;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private CatalogProtocolService service;
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = typeManager.getMapper(TYPE_MANAGER_CONTEXT_JSON_LD);
        var dspCallbackAddress = apiConfiguration.getDspCallbackAddress();
        var catalogController = new CatalogController(mapper, identityService, transformerRegistry, dspCallbackAddress, service);
        webService.registerResource(apiConfiguration.getContextAlias(), catalogController);
    }

    @Provider
    public DataService dataService() {
        return DataService.Builder.newInstance()
                .terms("connector")
                .endpointUrl(apiConfiguration.getDspCallbackAddress())
                .build();
    }
}

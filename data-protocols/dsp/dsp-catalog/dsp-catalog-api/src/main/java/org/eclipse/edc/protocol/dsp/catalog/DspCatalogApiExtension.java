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

package org.eclipse.edc.protocol.dsp.catalog;

import org.eclipse.edc.connector.contract.spi.offer.DatasetResolver;
import org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfiguration;
import org.eclipse.edc.protocol.dsp.catalog.controller.CatalogController;
import org.eclipse.edc.protocol.dsp.catalog.service.CatalogServiceImpl;
import org.eclipse.edc.protocol.dsp.spi.catalog.service.CatalogService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

@Extension(value = DspCatalogApiExtension.NAME)
@Provides({ CatalogService.class })
public class DspCatalogApiExtension implements ServiceExtension {
    
    public static final String NAME = "Dataspace Protocol Catalog Extension";
    
    @Inject
    private Monitor monitor;
    @Inject
    private WebService webService;
    @Inject
    private TypeManager typeManager;
    @Inject
    private DspApiConfiguration apiConfiguration;
    
    @Inject
    private DatasetResolver datasetResolver;
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var catalogService = new CatalogServiceImpl(datasetResolver);
        var catalogController = new CatalogController(monitor, catalogService, typeManager);
        webService.registerResource(apiConfiguration.getContextAlias(), catalogController);
    }
}

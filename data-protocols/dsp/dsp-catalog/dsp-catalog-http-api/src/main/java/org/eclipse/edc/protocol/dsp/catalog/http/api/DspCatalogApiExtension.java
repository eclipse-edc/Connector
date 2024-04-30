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

package org.eclipse.edc.protocol.dsp.catalog.http.api;

import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.catalog.http.api.controller.DspCatalogApiController;
import org.eclipse.edc.protocol.dsp.catalog.http.api.controller.DspCatalogApiController20241;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.Base64continuationTokenSerDes;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.ContinuationTokenManagerImpl;
import org.eclipse.edc.protocol.dsp.catalog.http.api.validation.CatalogRequestMessageValidator;
import org.eclipse.edc.protocol.dsp.http.spi.configuration.DspApiConfiguration;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1;

/**
 * Creates and registers the controller for dataspace protocol catalog requests.
 */
@Extension(value = DspCatalogApiExtension.NAME)
public class DspCatalogApiExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Catalog Extension";

    @Inject
    private WebService webService;
    @Inject
    private DspApiConfiguration apiConfiguration;
    @Inject
    private CatalogProtocolService service;
    @Inject
    private DataServiceRegistry dataServiceRegistry;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
    @Inject
    private DspRequestHandler dspRequestHandler;
    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;
    @Inject
    private ProtocolVersionRegistry versionRegistry;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private JsonLd jsonLd;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        validatorRegistry.register(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE, CatalogRequestMessageValidator.instance(criterionOperatorRegistry));

        var continuationTokenSerDes = new Base64continuationTokenSerDes(typeTransformerRegistry.forContext("dsp-api"), jsonLd);
        var catalogPaginationResponseDecoratorFactory = new ContinuationTokenManagerImpl(continuationTokenSerDes, context.getMonitor());
        webService.registerResource(apiConfiguration.getContextAlias(), new DspCatalogApiController(service, dspRequestHandler, catalogPaginationResponseDecoratorFactory));
        webService.registerResource(apiConfiguration.getContextAlias(), new DspCatalogApiController20241(service, dspRequestHandler, catalogPaginationResponseDecoratorFactory));

        dataServiceRegistry.register(DataService.Builder.newInstance()
                .endpointDescription("dspace:connector")
                .endpointUrl(apiConfiguration.getDspCallbackAddress())
                .build());

        versionRegistry.register(V_2024_1);
    }
}

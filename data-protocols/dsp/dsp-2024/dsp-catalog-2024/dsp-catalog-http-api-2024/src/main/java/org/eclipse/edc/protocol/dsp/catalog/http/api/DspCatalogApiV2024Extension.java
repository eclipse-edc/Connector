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
 *       Cofinity-X - make DSP versions pluggable
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.http.api;

import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.catalog.http.api.controller.DspCatalogApiController20241;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.Base64continuationTokenSerDes;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.ContinuationTokenManagerImpl;
import org.eclipse.edc.protocol.dsp.catalog.validation.CatalogRequestMessageValidator;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.protocol.ProtocolWebhookRegistry;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Creates and registers the controller for dataspace protocol v2024/1 catalog requests.
 */
@Extension(value = DspCatalogApiV2024Extension.NAME)
public class DspCatalogApiV2024Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Catalog v2024/1 Extension";

    @Inject
    private WebService webService;
    @Inject
    private ProtocolWebhookRegistry protocolWebhookRegistry;
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
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private Monitor monitor;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLd;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerValidators();

        webService.registerResource(ApiContext.PROTOCOL, new DspCatalogApiController20241(service, dspRequestHandler, continuationTokenManager(monitor)));
        webService.registerDynamicResource(ApiContext.PROTOCOL, DspCatalogApiController20241.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, DSP_SCOPE_V_2024_1));

        versionRegistry.register(V_2024_1);
    }

    @Override
    public void prepare() {
        registerDataService();
    }

    private void registerDataService() {
        var webhook = protocolWebhookRegistry.resolve(DATASPACE_PROTOCOL_HTTP_V_2024_1);
        if (webhook != null) {
            dataServiceRegistry.register(DATASPACE_PROTOCOL_HTTP_V_2024_1, DataService.Builder.newInstance()
                    .endpointDescription("dspace:connector")
                    .endpointUrl(webhook.url())
                    .build());
        }
    }

    private ContinuationTokenManager continuationTokenManager(Monitor monitor) {
        var continuationTokenSerDes = new Base64continuationTokenSerDes(transformerRegistry.forContext(DSP_TRANSFORMER_CONTEXT_V_2024_1), jsonLd);
        return new ContinuationTokenManagerImpl(continuationTokenSerDes, DSP_NAMESPACE_V_2024_1, monitor);
    }

    private void registerValidators() {
        validatorRegistry.register(DSP_NAMESPACE_V_2024_1.toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM), CatalogRequestMessageValidator.instance(criterionOperatorRegistry, DSP_NAMESPACE_V_2024_1));
    }
}

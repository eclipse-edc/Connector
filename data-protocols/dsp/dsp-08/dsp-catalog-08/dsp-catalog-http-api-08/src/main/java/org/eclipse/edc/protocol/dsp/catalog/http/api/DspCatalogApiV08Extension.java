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
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.catalog.http.api.controller.DspCatalogApiController08;
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

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_08;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_08;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Creates and registers the controller for dataspace protocol catalog requests.
 */
@Extension(value = DspCatalogApiV08Extension.NAME)
public class DspCatalogApiV08Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Catalog v08 Extension";

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
        registerValidators(DSP_NAMESPACE_V_08);

        webService.registerResource(ApiContext.PROTOCOL, new DspCatalogApiController08(service, dspRequestHandler, continuationTokenManager(monitor, DSP_TRANSFORMER_CONTEXT_V_08, DSP_NAMESPACE_V_08)));
        webService.registerDynamicResource(ApiContext.PROTOCOL, DspCatalogApiController08.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, DSP_SCOPE_V_08));
        
        versionRegistry.register(V_08);
    }

    @Override
    public void prepare() {
        registerDataService(DATASPACE_PROTOCOL_HTTP);
    }

    private void registerDataService(String protocol) {
        var webhook = protocolWebhookRegistry.resolve(protocol);
        if (webhook != null) {
            dataServiceRegistry.register(protocol, DataService.Builder.newInstance()
                    .endpointDescription("dspace:connector")
                    .endpointUrl(webhook.url())
                    .build());
        }
    }

    private ContinuationTokenManager continuationTokenManager(Monitor monitor, String version, JsonLdNamespace namespace) {
        var continuationTokenSerDes = new Base64continuationTokenSerDes(transformerRegistry.forContext(version), jsonLd);
        return new ContinuationTokenManagerImpl(continuationTokenSerDes, namespace, monitor);
    }

    private void registerValidators(JsonLdNamespace namespace) {
        validatorRegistry.register(namespace.toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM), CatalogRequestMessageValidator.instance(criterionOperatorRegistry, namespace));
    }
}

/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.http.api.v2025.virtual;

import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.Base64continuationTokenSerDes;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.ContinuationTokenManagerImpl;
import org.eclipse.edc.protocol.dsp.catalog.http.api.v2025.virtual.controller.DspCatalogApiController20251;
import org.eclipse.edc.protocol.dsp.catalog.validation.CatalogRequestMessageValidator;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.spi.http.DspVirtualSubResourceLocator;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import java.util.Optional;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_TRANSFORMER_CONTEXT_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_VERSION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;

/**
 * Creates and registers the controller for dataspace protocol v2025/1 catalog requests.
 */
@Extension(value = DspCatalogApi2025Extension.NAME)
public class DspCatalogApi2025Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol 2025/1 API Catalog Extension";

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
    private ProtocolWebhookResolver protocolWebhookResolver;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private Monitor monitor;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private ParticipantProfileResolver participantProfileResolver;
    @Inject
    private DataspaceProfileContextRegistry profileContextRegistry;

    @Inject
    private DspVirtualSubResourceLocator resourceLocator;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // Register validators and DataService factory for DSP 2025/1 profiles only (other DSP
        // versions are handled by their own extensions).
        profileContextRegistry.addRegistrationCallback(profile -> {
            if (!V_2025_1_VERSION.equals(profile.protocolVersion().version())) {
                return;
            }
            var ns = profile.protocolNamespace();
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM),
                    CatalogRequestMessageValidator.instance(criterionOperatorRegistry, ns));
            dataServiceRegistry.register(profile.name(), this::createDataService);
        });

        resourceLocator.registerSubResource("catalog", V_2025_1_VERSION, new DspCatalogApiController20251(service, participantContextService, participantProfileResolver, dspRequestHandler, continuationTokenManager(monitor)));

    }

    private ContinuationTokenManager continuationTokenManager(Monitor monitor) {
        var continuationTokenSerDes = new Base64continuationTokenSerDes(transformerRegistry.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1), jsonLd);
        return new ContinuationTokenManagerImpl(continuationTokenSerDes, DSP_NAMESPACE_V_2025_1, monitor);
    }

    private DataService createDataService(String participantContextId, String protocol) {
        return Optional.ofNullable(protocolWebhookResolver.getWebhook(participantContextId, protocol))
                .map(webhook -> DataService.Builder.newInstance()
                        .endpointDescription("dspace:connector")
                        .endpointUrl(webhook.url())
                        .build()).orElse(null);
    }
}

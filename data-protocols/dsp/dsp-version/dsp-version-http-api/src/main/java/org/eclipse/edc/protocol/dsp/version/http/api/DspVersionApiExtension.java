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

package org.eclipse.edc.protocol.dsp.version.http.api;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.version.http.api.transformer.JsonObjectFromProtocolVersionsTransformer;
import org.eclipse.edc.protocol.dsp.version.http.api.transformer.JsonObjectFromVersionsError;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.version.http.api.DspVersionApiExtension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provide API for the protocol versions.
 */
@Extension(NAME)
public class DspVersionApiExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Version Api";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private DspRequestHandler requestHandler;

    @Inject
    private VersionProtocolService service;

    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonLdMapper = typeManager.getMapper(JSON_LD);

        transformerRegistry.register(new JsonObjectFromProtocolVersionsTransformer());
        transformerRegistry.register(new JsonObjectFromVersionsError(Json.createBuilderFactory(Map.of())));

        webService.registerResource(ApiContext.PROTOCOL, new DspVersionApiController(requestHandler, service));
        webService.registerDynamicResource(ApiContext.PROTOCOL, DspVersionApiController.class, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, DSP_SCOPE_V_08));
    }

}

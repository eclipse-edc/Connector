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

package org.eclipse.edc.protocol.dsp.api.configuration;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;
import org.eclipse.edc.web.spi.provider.ObjectMapperProvider;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

/**
 * Provides the configuration for the Dataspace Protocol API context. Creates the API context
 * using {@link #DEFAULT_PROTOCOL_PORT} and {@link #DEFAULT_PROTOCOL_API_PATH}, if no respective
 * settings are provided. Configures the API context to allow Jakarta JSON-API types as endpoint
 * parameters.
 */
@Extension(value = DspApiConfigurationExtension.NAME)
@Provides({ DspApiConfiguration.class })
public class DspApiConfigurationExtension implements ServiceExtension {
    
    public static final String NAME = "Dataspace Protocol API Configuration Extension";
    
    public static final String CONTEXT_ALIAS = "protocol";
    
    public static final String DEFAULT_DSP_CALLBACK_ADDRESS = "http://localhost:8282/api/v1/dsp";
    public static final String DSP_CALLBACK_ADDRESS = "edc.dsp.callback.address";
    
    public static final int DEFAULT_PROTOCOL_PORT = 8282;
    public static final String DEFAULT_PROTOCOL_API_PATH = "/api/v1/dsp";
    
    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http.protocol")
            .contextAlias(CONTEXT_ALIAS)
            .defaultPath(DEFAULT_PROTOCOL_API_PATH)
            .defaultPort(DEFAULT_PROTOCOL_PORT)
            .name("Protocol API")
            .build();
    
    @Inject
    private TypeManager typeManager;

    @Inject
    private WebService webService;

    @Inject
    private WebServer webServer;

    @Inject
    private WebServiceConfigurer configurator;
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = configurator.configure(context, webServer, SETTINGS);
        var dspWebhookAddress = context.getSetting(DSP_CALLBACK_ADDRESS, DEFAULT_DSP_CALLBACK_ADDRESS);
        context.registerService(DspApiConfiguration.class, new DspApiConfiguration(config.getContextAlias(), dspWebhookAddress));
        
        var jsonLdMapper = typeManager.getMapper(JSON_LD);
        webService.registerResource(config.getContextAlias(), new ObjectMapperProvider(jsonLdMapper));
    }
    
}

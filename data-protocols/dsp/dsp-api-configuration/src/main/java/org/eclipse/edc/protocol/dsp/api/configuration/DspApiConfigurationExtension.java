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

import org.eclipse.edc.protocol.dsp.api.configuration.serdes.JsonLdObjectMapperProvider;
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

@Extension(value = DspApiConfigurationExtension.NAME)
@Provides({ DspApiConfiguration.class })
public class DspApiConfigurationExtension implements ServiceExtension {
    
    public static final String NAME = "Dataspace Protocol API Configuration Extension";
    
    private static final String DEFAULT_DSP_WEBHOOK_ADDRESS = "http://localhost";
    private static final String DSP_WEBHOOK_ADDRESS = "dsp.webhook.address";
    
    private static final int DEFAULT_PROTOCOL_PORT = 8282;
    private static final String DEFAULT_PROTOCOL_API_PATH = "/api/v1/dsp";
    
    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http.protocol")
            .contextAlias("protocol")
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
        var idsWebhookAddress = context.getSetting(DSP_WEBHOOK_ADDRESS, DEFAULT_DSP_WEBHOOK_ADDRESS);
        context.registerService(DspApiConfiguration.class, new DspApiConfiguration(config.getContextAlias(), idsWebhookAddress));
        
        webService.registerResource(config.getContextAlias(), new JsonLdObjectMapperProvider(typeManager.getMapper("json-ld")));
    }

}

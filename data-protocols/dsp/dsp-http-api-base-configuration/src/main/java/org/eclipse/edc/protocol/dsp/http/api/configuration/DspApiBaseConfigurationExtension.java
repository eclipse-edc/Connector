/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.api.configuration;

import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(DspApiBaseConfigurationExtension.NAME)
public class DspApiBaseConfigurationExtension implements ServiceExtension {
    
    public static final String NAME = "Dataspace Protocol API Base Configuration Extension";
    
    static final String DEFAULT_PROTOCOL_PATH = "/api/protocol";
    static final int DEFAULT_PROTOCOL_PORT = 8282;
    
    @Setting(description = "Configures endpoint for reaching the Protocol API in the form \"<hostname:protocol.port/protocol.path>\"", key = "edc.dsp.callback.address", required = false)
    private String callbackAddress;
    
    @Configuration
    private DspApiConfiguration apiConfiguration;
    
    @Inject
    private Hostname hostname;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private WebService webService;
    @Inject
    private TypeManager typeManager;
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var portMapping = new PortMapping(ApiContext.PROTOCOL, apiConfiguration.port(), apiConfiguration.path());
        portMappingRegistry.register(portMapping);
        
        webService.registerResource(ApiContext.PROTOCOL, new ObjectMapperProvider(typeManager, JSON_LD));
    }
    
    @Override
    public void prepare() {
        var mapper = typeManager.getMapper(JSON_LD);
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class);
    }
    
    @Provider
    public DspBaseWebhookAddress dspBaseWebhookAddress() {
        var dspWebhookAddress = ofNullable(callbackAddress)
                .orElseGet(() -> format("http://%s:%s%s", hostname.get(), apiConfiguration.port(), apiConfiguration.path()));
        
        return () -> dspWebhookAddress;
    }
    
    @Settings
    record DspApiConfiguration(
            @Setting(key = "web.http." + ApiContext.PROTOCOL + ".port", description = "Port for " + ApiContext.PROTOCOL + " api context", defaultValue = DEFAULT_PROTOCOL_PORT + "")
            int port,
            @Setting(key = "web.http." + ApiContext.PROTOCOL + ".path", description = "Path for " + ApiContext.PROTOCOL + " api context", defaultValue = DEFAULT_PROTOCOL_PATH)
            String path
    ) {
    
    }
}

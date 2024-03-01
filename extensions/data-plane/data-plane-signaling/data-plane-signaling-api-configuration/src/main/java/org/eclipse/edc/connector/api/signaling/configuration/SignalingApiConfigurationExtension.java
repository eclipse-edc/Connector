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

package org.eclipse.edc.connector.api.signaling.configuration;

import jakarta.json.Json;
import org.eclipse.edc.connector.api.signaling.transform.SignalingApiTransformerRegistry;
import org.eclipse.edc.connector.api.signaling.transform.SignalingApiTransformerRegistryImpl;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowTerminateMessageTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.util.Map;

import static org.eclipse.edc.connector.api.signaling.configuration.SignalingApiConfigurationExtension.NAME;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

@Provides(SignalingApiConfiguration.class)
@Extension(value = NAME)
public class SignalingApiConfigurationExtension implements ServiceExtension {
    public static final String NAME = "DataPlane Signaling API Configuration Extension";
    public static final String WEB_SERVICE_NAME = "DataPlane Signaling API";

    private static final String SIGNALING_CONTEXT_ALIAS = "signaling";
    private static final String DEFAULT_SIGNALING_API_CONTEXT_PATH = "/api/signaling";
    private static final int DEFAULT_SIGNALING_API_PORT = 10080;
    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + SIGNALING_CONTEXT_ALIAS)
            .contextAlias(SIGNALING_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_SIGNALING_API_CONTEXT_PATH)
            .defaultPort(DEFAULT_SIGNALING_API_PORT)
            .useDefaultContext(true)
            .name(WEB_SERVICE_NAME)
            .build();
    private static final String SIGNALING_SCOPE = "SIGNALING_API";

    @Inject
    private WebService webService;
    @Inject
    private WebServiceConfigurer configurer;
    @Inject
    private WebServer webServer;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var webServiceConfiguration = configurer.configure(context, webServer, SETTINGS);
        context.registerService(SignalingApiConfiguration.class, new SignalingApiConfiguration(webServiceConfiguration));

        jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, SIGNALING_SCOPE);
        var jsonLdMapper = typeManager.getMapper(JSON_LD);
        webService.registerResource(webServiceConfiguration.getContextAlias(), new ObjectMapperProvider(jsonLdMapper));
        webService.registerResource(webServiceConfiguration.getContextAlias(), new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, SIGNALING_SCOPE));
    }

    @Provider
    public SignalingApiTransformerRegistry managementApiTypeTransformerRegistry() {
        var factory = Json.createBuilderFactory(Map.of());

        var registry = new SignalingApiTransformerRegistryImpl(this.transformerRegistry);
        registry.register(new JsonObjectToDataFlowSuspendMessageTransformer());
        registry.register(new JsonObjectToDataFlowTerminateMessageTransformer());
        registry.register(new JsonObjectToDataAddressTransformer());
        return registry;
    }
}

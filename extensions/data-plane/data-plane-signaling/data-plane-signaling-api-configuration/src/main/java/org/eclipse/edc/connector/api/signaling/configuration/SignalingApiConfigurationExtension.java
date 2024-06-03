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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowResponseMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowTerminateMessageTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.dspace.from.JsonObjectFromDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.dspace.to.JsonObjectToDataAddressDspaceTransformer;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.eclipse.edc.connector.api.signaling.configuration.SignalingApiConfigurationExtension.NAME;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Deprecated(since = "0.6.4")
@Extension(value = NAME)
public class SignalingApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "DataPlane Signaling API Configuration Extension";

    @SettingContext("Signaling API context setting key")
    private static final String SIGNALING_CONFIG_KEY = "web.http." + ApiContext.SIGNALING;

    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey(SIGNALING_CONFIG_KEY)
            .contextAlias(ApiContext.SIGNALING)
            .defaultPath("/api/signaling")
            .defaultPort(10080)
            .useDefaultContext(false)
            .name("DataPlane Signaling API")
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
        var warningMessage = """
                The data-plane-signaling-api-configuration extension is deprecated as the related 'web.http.signaling'
                settings, please exclude from your build and configure your endpoints to the control-api context
                """;
        context.getMonitor().warning(warningMessage);
        var config = context.getConfig(SIGNALING_CONFIG_KEY);
        configurer.configure(config, webServer, SETTINGS);

        jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, SIGNALING_SCOPE);
        jsonLd.registerNamespace(DSPACE_PREFIX, DSPACE_SCHEMA, SIGNALING_SCOPE);

        var jsonLdMapper = getJsonLdMapper();
        webService.registerResource(ApiContext.SIGNALING, new ObjectMapperProvider(jsonLdMapper));
        webService.registerResource(ApiContext.SIGNALING, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, SIGNALING_SCOPE));

        var factory = Json.createBuilderFactory(Map.of());

        var signalingApiTypeTransformerRegistry = transformerRegistry.forContext("signaling-api");
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataFlowStartMessageTransformer());
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataFlowSuspendMessageTransformer());
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataFlowTerminateMessageTransformer());
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataAddressDspaceTransformer());
        signalingApiTypeTransformerRegistry.register(new JsonObjectFromDataFlowResponseMessageTransformer(factory));
        signalingApiTypeTransformerRegistry.register(new JsonObjectFromDataAddressDspaceTransformer(factory, jsonLdMapper));
    }

    @NotNull
    private ObjectMapper getJsonLdMapper() {
        return typeManager.getMapper(JSON_LD);
    }
}

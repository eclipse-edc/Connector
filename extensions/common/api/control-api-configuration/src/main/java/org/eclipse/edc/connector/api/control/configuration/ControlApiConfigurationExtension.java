/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.control.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Tells all the Control API controllers under which context alias they need to register their resources: either
 * `default` or `control`
 */
@Extension(value = ControlApiConfigurationExtension.NAME)
@Provides(ControlApiUrl.class)
public class ControlApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Control API configuration";
    static final String CONTROL_SCOPE = "CONTROL_API";
    static final int DEFAULT_CONTROL_PORT = 9191;
    static final String DEFAULT_CONTROL_PATH = "/api/control";
    private static final String API_VERSION_JSON_FILE = "control-api-version.json";

    @Setting(description = "Configures endpoint for reaching the Control API. If it's missing it defaults to the hostname configuration.", key = "edc.control.endpoint", required = false)
    private String controlEndpoint;
    @Configuration
    private ControlApiConfiguration apiConfiguration;

    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private WebService webService;
    @Inject
    private Hostname hostname;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ApiAuthenticationRegistry authenticationRegistry;
    @Inject
    private ApiVersionService apiVersionService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var portMapping = new PortMapping(ApiContext.CONTROL, apiConfiguration.port(), apiConfiguration.path());
        portMappingRegistry.register(portMapping);
        context.registerService(ControlApiUrl.class, controlApiUrl(context, portMapping));

        jsonLd.registerNamespace(EDC_PREFIX, EDC_NAMESPACE, CONTROL_SCOPE);
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE, CONTROL_SCOPE);
        jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, CONTROL_SCOPE);
        jsonLd.registerNamespace(DSPACE_PREFIX, DSPACE_SCHEMA, CONTROL_SCOPE);

        webService.registerResource(ApiContext.CONTROL, new ObjectMapperProvider(typeManager, JSON_LD));
        webService.registerResource(ApiContext.CONTROL, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, CONTROL_SCOPE));

        registerVersionInfo(getClass().getClassLoader());
    }

    private void registerVersionInfo(ClassLoader resourceClassLoader) {
        try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
            if (versionContent == null) {
                throw new EdcException("Version file not found or not readable.");
            }
            Stream.of(typeManager.getMapper()
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .readValue(versionContent, VersionRecord[].class))
                    .forEach(vr -> apiVersionService.addRecord(ApiContext.CONTROL, vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private ControlApiUrl controlApiUrl(ServiceExtensionContext context, PortMapping config) {
        var callbackAddress = ofNullable(controlEndpoint).orElseGet(() -> format("http://%s:%s%s", hostname.get(), config.port(), config.path()));

        try {
            var url = URI.create(callbackAddress);
            return () -> url;
        } catch (IllegalArgumentException e) {
            context.getMonitor().severe("Error creating control plane endpoint url", e);
            throw new EdcException(e);
        }
    }

    @Settings
    record ControlApiConfiguration(
            @Setting(key = "web.http." + ApiContext.CONTROL + ".port", description = "Port for " + ApiContext.CONTROL + " api context", defaultValue = DEFAULT_CONTROL_PORT + "")
            int port,
            @Setting(key = "web.http." + ApiContext.CONTROL + ".path", description = "Path for " + ApiContext.CONTROL + " api context", defaultValue = DEFAULT_CONTROL_PATH)
            String path
    ) {

    }
}

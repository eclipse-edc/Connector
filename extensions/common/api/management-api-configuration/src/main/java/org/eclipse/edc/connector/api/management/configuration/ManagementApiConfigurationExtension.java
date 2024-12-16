/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.json.Json;
import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.connector.api.management.configuration.transform.JsonObjectFromContractAgreementTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.OdrlTransformersFactory;
import org.eclipse.edc.connector.controlplane.transform.odrl.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
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
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.eclipse.edc.web.spi.configuration.context.ManagementApiUrl;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_V_4_ALPHA;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Configure 'management' api context.
 */
@Provides(ManagementApiUrl.class)
@Extension(ManagementApiConfigurationExtension.NAME)
public class ManagementApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Management API configuration";
    static final String MANAGEMENT_SCOPE = "MANAGEMENT_API";
    static final int DEFAULT_MANAGEMENT_PORT = 8181;
    static final String DEFAULT_MANAGEMENT_PATH = "/api/management";
    private static final String API_VERSION_JSON_FILE = "management-api-version.json";
    private static final boolean DEFAULT_MANAGEMENT_API_ENABLE_CONTEXT = false;

    @Setting(description = "Configures endpoint for reaching the Management API, in the format \"<hostname:management.port/management.path>\"", key = "edc.management.endpoint", required = false)
    private String managementApiEndpoint;
    @Setting(description = "If set enable the usage of management api JSON-LD context.", defaultValue = "" + DEFAULT_MANAGEMENT_API_ENABLE_CONTEXT, key = "edc.management.context.enabled")
    private boolean managementApiContextEnabled;
    @Configuration
    private ManagementApiConfiguration apiConfiguration;

    @Inject
    private WebService webService;
    @Inject
    private ApiAuthenticationRegistry authenticationRegistry;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private ParticipantIdMapper participantIdMapper;
    @Inject
    private Hostname hostname;

    @Inject
    private ApiVersionService apiVersionService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var portMapping = new PortMapping(ApiContext.MANAGEMENT, apiConfiguration.port(), apiConfiguration.path());
        portMappingRegistry.register(portMapping);

        context.registerService(ManagementApiUrl.class, managementApiUrl(context, portMapping));

        var authenticationFilter = new AuthenticationRequestFilter(authenticationRegistry, "management-api");
        webService.registerResource(ApiContext.MANAGEMENT, authenticationFilter);

        if (managementApiContextEnabled) {
            jsonLd.registerContext(EDC_CONNECTOR_MANAGEMENT_CONTEXT, MANAGEMENT_SCOPE);
        } else {
            jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE, MANAGEMENT_SCOPE);
            jsonLd.registerNamespace(EDC_PREFIX, EDC_NAMESPACE, MANAGEMENT_SCOPE);
            jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, MANAGEMENT_SCOPE);
        }

        var jsonLdMapper = typeManager.getMapper(JSON_LD);
        webService.registerResource(ApiContext.MANAGEMENT, new ObjectMapperProvider(jsonLdMapper));
        webService.registerResource(ApiContext.MANAGEMENT, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, MANAGEMENT_SCOPE));

        var managementApiTransformerRegistry = transformerRegistry.forContext(MANAGEMENT_API_CONTEXT);

        var factory = Json.createBuilderFactory(Map.of());
        managementApiTransformerRegistry.register(new JsonObjectFromContractAgreementTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectFromAssetTransformer(factory, jsonLdMapper));
        managementApiTransformerRegistry.register(new JsonObjectFromPolicyTransformer(factory, participantIdMapper));
        managementApiTransformerRegistry.register(new JsonObjectFromQuerySpecTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectFromCriterionTransformer(factory, jsonLdMapper));

        OdrlTransformersFactory.jsonObjectToOdrlTransformers(participantIdMapper).forEach(managementApiTransformerRegistry::register);
        managementApiTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToCriterionTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToAssetTransformer());
        managementApiTransformerRegistry.register(new JsonValueToGenericTypeTransformer(jsonLdMapper));

        var managementApiTransformerRegistryV4Alpha = managementApiTransformerRegistry.forContext(MANAGEMENT_API_V_4_ALPHA);

        managementApiTransformerRegistryV4Alpha.register(new JsonObjectFromPolicyTransformer(factory, participantIdMapper, true));

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
                    .forEach(vr -> apiVersionService.addRecord(ApiContext.MANAGEMENT, vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private ManagementApiUrl managementApiUrl(ServiceExtensionContext context, PortMapping config) {
        var callbackAddress = ofNullable(managementApiEndpoint).orElseGet(() -> format("http://%s:%s%s", hostname.get(), config.port(), config.path()));
        try {
            var url = URI.create(callbackAddress);
            return () -> url;
        } catch (IllegalArgumentException e) {
            context.getMonitor().severe("Error creating management plane endpoint url", e);
            throw new EdcException(e);
        }
    }

    @Settings
    record ManagementApiConfiguration(
            @Setting(key = "web.http." + ApiContext.MANAGEMENT + ".port", description = "Port for " + ApiContext.MANAGEMENT + " api context", defaultValue = DEFAULT_MANAGEMENT_PORT + "")
            int port,
            @Setting(key = "web.http." + ApiContext.MANAGEMENT + ".path", description = "Path for " + ApiContext.MANAGEMENT + " api context", defaultValue = DEFAULT_MANAGEMENT_PATH)
            String path
    ) {

    }
}

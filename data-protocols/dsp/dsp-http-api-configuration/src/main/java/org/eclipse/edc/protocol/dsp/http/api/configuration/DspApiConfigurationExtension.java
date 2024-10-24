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

package org.eclipse.edc.protocol.dsp.http.api.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.transform.edc.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.OdrlTransformersFactory;
import org.eclipse.edc.connector.controlplane.transform.odrl.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.dspace.from.JsonObjectFromDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.dspace.to.JsonObjectToDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.dspace.v2024.from.JsonObjectFromDataAddressDspace2024Transformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.util.Map;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_2024_1;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Configure 'protocol' api context.
 */
@Extension(value = DspApiConfigurationExtension.NAME)
@Provides(ProtocolWebhook.class)
public class DspApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol API Configuration Extension";

    @Setting(value = "Configures endpoint for reaching the Protocol API.", defaultValue = "<hostname:protocol.port/protocol.path>")
    public static final String DSP_CALLBACK_ADDRESS = "edc.dsp.callback.address";
    
    @SettingContext("Protocol API context setting key")
    private static final String PROTOCOL_CONFIG_KEY = "web.http." + ApiContext.PROTOCOL;

    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey(PROTOCOL_CONFIG_KEY)
            .contextAlias(ApiContext.PROTOCOL)
            .defaultPath("/api/v1/dsp")
            .defaultPort(8282)
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
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private ParticipantIdMapper participantIdMapper;
    @Inject
    private Hostname hostname;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var contextConfig = context.getConfig(PROTOCOL_CONFIG_KEY);
        var apiConfiguration = configurator.configure(contextConfig, webServer, SETTINGS);
        var dspWebhookAddress = context.getSetting(DSP_CALLBACK_ADDRESS, format("http://%s:%s%s", hostname.get(), apiConfiguration.getPort(), apiConfiguration.getPath()));
        context.registerService(ProtocolWebhook.class, () -> dspWebhookAddress);

        var jsonLdMapper = typeManager.getMapper(JSON_LD);

        // registers ns for DSP scope
        registerNamespaces(DSP_SCOPE_V_08, DSP_NAMESPACE_V_08);
        registerNamespaces(DSP_SCOPE_V_2024_1, DSP_NAMESPACE_V_2024_1);

        webService.registerResource(ApiContext.PROTOCOL, new ObjectMapperProvider(jsonLdMapper));

        var mapper = typeManager.getMapper(JSON_LD);
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class);

        registerV08Transformers(mapper);
        registerV2024Transformers(mapper);
        registerTransformers(DSP_TRANSFORMER_CONTEXT_V_08, DSP_NAMESPACE_V_08, mapper);
        registerTransformers(DSP_TRANSFORMER_CONTEXT_V_2024_1, DSP_NAMESPACE_V_2024_1, mapper);
    }

    private void registerNamespaces(String scope, JsonLdNamespace dspNamespace) {
        jsonLd.registerNamespace(DCAT_PREFIX, DCAT_SCHEMA, scope);
        jsonLd.registerNamespace(DCT_PREFIX, DCT_SCHEMA, scope);
        jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, scope);
        jsonLd.registerNamespace(DSPACE_PREFIX, dspNamespace.namespace(), scope);
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE, scope);
        jsonLd.registerNamespace(EDC_PREFIX, EDC_NAMESPACE, scope);
    }

    private void registerTransformers(String version, JsonLdNamespace dspNamespace, ObjectMapper mapper) {
        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());

        // EDC model to JSON-LD transformers
        var dspApiTransformerRegistry = transformerRegistry.forContext(version);
        dspApiTransformerRegistry.register(new JsonObjectFromPolicyTransformer(jsonBuilderFactory, participantIdMapper));
        dspApiTransformerRegistry.register(new JsonObjectFromAssetTransformer(jsonBuilderFactory, mapper));
        dspApiTransformerRegistry.register(new JsonObjectFromQuerySpecTransformer(jsonBuilderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromCriterionTransformer(jsonBuilderFactory, mapper));

        // JSON-LD to EDC model transformers
        // ODRL Transformers
        OdrlTransformersFactory.jsonObjectToOdrlTransformers(participantIdMapper).forEach(dspApiTransformerRegistry::register);

        dspApiTransformerRegistry.register(new JsonValueToGenericTypeTransformer(mapper));
        dspApiTransformerRegistry.register(new JsonObjectToAssetTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToCriterionTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToDataAddressDspaceTransformer(dspNamespace));
    }


    private void registerV08Transformers(ObjectMapper mapper) {
        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());

        // EDC model to JSON-LD transformers
        var dspApiTransformerRegistry = transformerRegistry.forContext(DSP_TRANSFORMER_CONTEXT_V_08);

        dspApiTransformerRegistry.register(new JsonObjectFromDataAddressDspaceTransformer(jsonBuilderFactory, mapper));

    }

    private void registerV2024Transformers(ObjectMapper mapper) {
        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());

        // EDC model to JSON-LD transformers
        var dspApiTransformerRegistry = transformerRegistry.forContext(DSP_TRANSFORMER_CONTEXT_V_2024_1);

        dspApiTransformerRegistry.register(new JsonObjectFromDataAddressDspace2024Transformer(jsonBuilderFactory, mapper));

    }
}

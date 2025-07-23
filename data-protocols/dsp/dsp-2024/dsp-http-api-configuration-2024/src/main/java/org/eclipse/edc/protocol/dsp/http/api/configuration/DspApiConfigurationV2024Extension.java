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
 *       Cofinity-X - extract webhook and port mapping creation
 *
 */

package org.eclipse.edc.protocol.dsp.http.api.configuration;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.transform.edc.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.OdrlTransformersFactory;
import org.eclipse.edc.connector.controlplane.transform.odrl.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.dspace.to.JsonObjectToDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.dspace.v2024.from.JsonObjectFromDataAddressDspace2024Transformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;

import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.DATASPACE_PROTOCOL_HTTP_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.DSP_SCOPE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.DSP_TRANSFORMER_CONTEXT_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.V_2024_1_PATH;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Registers protocol webhook, API transformers and namespaces for DSP v2024/1.
 */
@Extension(value = DspApiConfigurationV2024Extension.NAME)
public class DspApiConfigurationV2024Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol API Configuration v2024/1 Extension";

    private static final boolean DEFAULT_WELL_KNOWN_PATH = false;

    @Setting(description = "If set enable the well known path resolution scheme will be used", key = "edc.dsp.well-known-path.enabled", required = false, defaultValue = DEFAULT_WELL_KNOWN_PATH + "")
    private boolean wellKnownPathEnabled;

    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private ParticipantIdMapper participantIdMapper;
    @Inject
    private DspBaseWebhookAddress dspWebhookAddress;
    @Inject
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var v2024Path = dspWebhookAddress.get() + (wellKnownPathEnabled ? "" : V_2024_1_PATH);

        dataspaceProfileContextRegistry.registerDefault(new DataspaceProfileContext(DATASPACE_PROTOCOL_HTTP_V_2024_1, V_2024_1, () -> v2024Path));

        // registers ns for DSP scope
        registerNamespaces();
        registerTransformers();
    }

    private void registerNamespaces() {
        jsonLd.registerNamespace(DCAT_PREFIX, DCAT_SCHEMA, DSP_SCOPE_V_2024_1);
        jsonLd.registerNamespace(DCT_PREFIX, DCT_SCHEMA, DSP_SCOPE_V_2024_1);
        jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, DSP_SCOPE_V_2024_1);
        jsonLd.registerNamespace(DSPACE_PREFIX, DSP_NAMESPACE_V_2024_1.namespace(), DSP_SCOPE_V_2024_1);
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE, DSP_SCOPE_V_2024_1);
        jsonLd.registerNamespace(EDC_PREFIX, EDC_NAMESPACE, DSP_SCOPE_V_2024_1);
    }

    private void registerTransformers() {
        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());

        // EDC model to JSON-LD transformers
        var dspApiTransformerRegistry = transformerRegistry.forContext(DSP_TRANSFORMER_CONTEXT_V_2024_1);
        dspApiTransformerRegistry.register(new JsonObjectFromAssetTransformer(jsonBuilderFactory, typeManager, JSON_LD));
        dspApiTransformerRegistry.register(new JsonObjectFromQuerySpecTransformer(jsonBuilderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromCriterionTransformer(jsonBuilderFactory, typeManager, JSON_LD));

        // JSON-LD to EDC model transformers
        // ODRL Transformers
        OdrlTransformersFactory.jsonObjectToOdrlTransformers(participantIdMapper).forEach(dspApiTransformerRegistry::register);

        dspApiTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, JSON_LD));
        dspApiTransformerRegistry.register(new JsonObjectToAssetTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToCriterionTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToDataAddressDspaceTransformer(DSP_NAMESPACE_V_2024_1));

        dspApiTransformerRegistry.register(new JsonObjectFromPolicyTransformer(jsonBuilderFactory, participantIdMapper, new JsonObjectFromPolicyTransformer.TransformerConfig(true, false)));
        dspApiTransformerRegistry.register(new JsonObjectFromDataAddressDspace2024Transformer(jsonBuilderFactory, typeManager, JSON_LD, DSP_NAMESPACE_V_2024_1));
    }
}

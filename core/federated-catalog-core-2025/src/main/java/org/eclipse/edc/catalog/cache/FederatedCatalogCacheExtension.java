/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - Add shutdown method
 *
 */

package org.eclipse.edc.catalog.cache;

import jakarta.json.Json;
import org.eclipse.edc.catalog.cache.query.DspCatalogRequestAction;
import org.eclipse.edc.catalog.transform.JsonObjectToCatalogTransformer;
import org.eclipse.edc.catalog.transform.JsonObjectToDataServiceTransformer;
import org.eclipse.edc.catalog.transform.JsonObjectToDatasetTransformer;
import org.eclipse.edc.catalog.transform.JsonObjectToDistributionTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToPolicyTransformer;
import org.eclipse.edc.crawler.spi.CrawlerActionRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.v2025.from.JsonObjectFromCatalogV2025Transformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_TRANSFORMER_CONTEXT_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = FederatedCatalogCacheExtension.NAME)
public class FederatedCatalogCacheExtension implements ServiceExtension {

    public static final String NAME = "Federated Catalog Cache DSP 2025/1";

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    @Inject
    private CrawlerActionRegistry crawlerActionRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ParticipantIdMapper participantIdMapper;
    @Inject
    private TypeTransformerRegistry registry;
    @Inject
    private JsonLd jsonLdService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerTransformers();

        var mapper = typeManager.getMapper(JSON_LD);
        var dspTransformerRegistry = registry.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
        var adapter = new DspCatalogRequestAction(dispatcherRegistry, participantContextSupplier, context.getMonitor(), mapper, dspTransformerRegistry, jsonLdService);
        crawlerActionRegistry.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, adapter);
    }

    private void registerTransformers() {
        transformerRegistry.register(new JsonObjectToCatalogTransformer());
        transformerRegistry.register(new JsonObjectToDatasetTransformer());
        transformerRegistry.register(new JsonObjectToDataServiceTransformer());
        transformerRegistry.register(new JsonObjectToDistributionTransformer());
        transformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        transformerRegistry.register(new JsonObjectToCriterionTransformer());

        var jsonFactory = Json.createBuilderFactory(Map.of());
        transformerRegistry.register(new JsonObjectFromCatalogV2025Transformer(jsonFactory, typeManager, JSON_LD, participantIdMapper, DSP_NAMESPACE_V_2025_1));
        transformerRegistry.register(new JsonObjectFromDatasetTransformer(jsonFactory, typeManager, JSON_LD));
        transformerRegistry.register(new JsonObjectFromDistributionTransformer(jsonFactory));
        transformerRegistry.register(new JsonObjectFromDataServiceTransformer(jsonFactory));

        transformerRegistry.register(new JsonObjectFromPolicyTransformer(jsonFactory, participantIdMapper));
        transformerRegistry.register(new JsonObjectToPolicyTransformer(participantIdMapper));
        transformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, JSON_LD));
    }

}

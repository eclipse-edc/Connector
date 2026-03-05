/*
 *  Copyright (c) 2025 Microsoft Corporation
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

package org.eclipse.edc.catalog;

import jakarta.json.Json;
import org.eclipse.edc.catalog.transform.JsonObjectToCatalogTransformer;
import org.eclipse.edc.catalog.transform.JsonObjectToDataServiceTransformer;
import org.eclipse.edc.catalog.transform.JsonObjectToDatasetTransformer;
import org.eclipse.edc.catalog.transform.JsonObjectToDistributionTransformer;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.connector.controlplane.transform.odrl.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToPolicyTransformer;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.v2025.from.JsonObjectFromCatalogV2025Transformer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

public class TestUtil {

    public static final String TEST_PROTOCOL = "test-protocol";

    public static Catalog createCatalog(String id) {
        var dataService = DataService.Builder.newInstance()
                .endpointUrl("https://test-dataservice.endpoint.url")
                .endpointDescription("test endpoint description")
                .build();
        return buildCatalog(id)
                .datasets(List.of(Dataset.Builder.newInstance()
                        .id(id + "-dataset")
                        .distributions(List.of(Distribution.Builder.newInstance()
                                .dataService(dataService)
                                .format("test-format").build()))
                        .build()))
                .dataServices(List.of(dataService))
                .build();
    }

    public static Catalog.Builder buildCatalog(String id) {
        return Catalog.Builder.newInstance()
                .participantId("test-participant")
                .id(id)
                .properties(new HashMap<>());
    }

    @NotNull
    public static TargetNode createNode() {
        return new TargetNode("testnode" + UUID.randomUUID(), "did:web:" + UUID.randomUUID(), "http://test.com", List.of(TEST_PROTOCOL));
    }

    public static Catalog createCatalog(int howManyOffers) {
        var datasets = IntStream.range(0, howManyOffers)
                .mapToObj(i -> createDataset("dataset-" + i))
                .collect(Collectors.toList());

        var build = List.of(DataService.Builder.newInstance().build());
        return Catalog.Builder.newInstance().participantId("test-participant").id("catalog").datasets(datasets).dataServices(build).build();
    }

    public static Dataset createDataset(String id) {
        return Dataset.Builder.newInstance()
                .offer("test-offer", Policy.Builder.newInstance().build())
                .distribution(Distribution.Builder.newInstance().format("test-format").dataService(DataService.Builder.newInstance().build()).build())
                .id(id)
                .build();
    }

    // registers all the necessary transformers to avoid duplicating their behaviour in mocks
    public static void registerTransformers(TypeTransformerRegistry registry) {
        var factory = Json.createBuilderFactory(Map.of());
        var typeManager = new JacksonTypeManager();
        var participantIdMapper = new NoOpParticipantIdMapper();
        registry.register(new JsonObjectFromCatalogV2025Transformer(factory, typeManager, JSON_LD, participantIdMapper, DSP_NAMESPACE_V_2025_1));

        registry.register(new JsonObjectFromDatasetTransformer(factory, typeManager, JSON_LD));
        registry.register(new JsonObjectFromDataServiceTransformer(factory));
        registry.register(new JsonObjectFromPolicyTransformer(factory, participantIdMapper));
        registry.register(new JsonObjectFromDistributionTransformer(factory));
        registry.register(new JsonObjectToCatalogTransformer());
        registry.register(new JsonObjectToDatasetTransformer());
        registry.register(new JsonObjectToDataServiceTransformer());
        registry.register(new JsonObjectToPolicyTransformer(participantIdMapper));
        registry.register(new JsonObjectToDistributionTransformer());
    }

    public static class NoOpParticipantIdMapper implements ParticipantIdMapper {
        @Override
        public String toIri(String id) {
            return id;
        }

        @Override
        public String fromIri(String id) {
            return id;
        }
    }
}
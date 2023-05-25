/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.catalog;

import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.asset.AssetEntry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Duration;
import java.util.stream.Stream;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyMap;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.Mockito.mock;

@ExtendWith(EdcExtension.class)
class DatasetResolverImplPerformanceTest {

    private final Clock clock = Clock.systemUTC();

    @NotNull
    private static PolicyDefinition.Builder createPolicyDefinition(String id) {
        return PolicyDefinition.Builder.newInstance().id(id).policy(Policy.Builder.newInstance().build());
    }

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.registerServiceMock(ProtocolWebhook.class, mock(ProtocolWebhook.class));
        extension.registerServiceMock(DataPlaneInstanceStore.class, mock(DataPlaneInstanceStore.class));
    }

    @Test
    void oneAssetPerDefinition(DatasetResolver datasetResolver, ContractDefinitionStore contractDefinitionStore, AssetIndex assetIndex, PolicyDefinitionStore policyDefinitionStore) {
        policyDefinitionStore.create(createPolicyDefinition("policy").build());
        range(0, 10000).mapToObj(i -> createContractDefinition(String.valueOf(i))
                .accessPolicyId("policy")
                .contractPolicyId("policy")
                .assetsSelectorCriterion(criterion(Asset.PROPERTY_ID, "=", String.valueOf(i))).build()
                ).forEach(contractDefinitionStore::save);
        range(0, 10000).mapToObj(i -> createAsset(String.valueOf(i)).build()).map(this::createAssetEntry).forEach(assetIndex::create);

        var firstPageQuery = QuerySpec.Builder.newInstance().offset(0).limit(100).build();
        var firstPageDatasets = queryDatasetsIn(datasetResolver, firstPageQuery, ofSeconds(1));

        assertThat(firstPageDatasets).hasSize(100);

        var lastPageQuery = QuerySpec.Builder.newInstance().offset(9900).limit(100).build();
        var lastPageDatasets = queryDatasetsIn(datasetResolver, lastPageQuery, ofSeconds(1));

        assertThat(lastPageDatasets).hasSize(100);
    }

    @Test
    void fewDefinitionsSelectAllAssets(DatasetResolver datasetResolver, ContractDefinitionStore contractDefinitionStore, AssetIndex assetIndex, PolicyDefinitionStore policyDefinitionStore) {
        policyDefinitionStore.create(createPolicyDefinition("policy").build());
        range(0, 10).mapToObj(i -> createContractDefinition(String.valueOf(i)).accessPolicyId("policy").contractPolicyId("policy").build()).forEach(contractDefinitionStore::save);
        range(0, 10000).mapToObj(i -> createAsset(String.valueOf(i)).build()).map(this::createAssetEntry).forEach(assetIndex::create);

        var firstPageQuery = QuerySpec.Builder.newInstance().offset(0).limit(100).build();
        var firstPageDatasets = queryDatasetsIn(datasetResolver, firstPageQuery, ofSeconds(1));

        assertThat(firstPageDatasets).hasSize(100);

        var lastPageQuery = QuerySpec.Builder.newInstance().offset(9900).limit(100).build();
        var lastPageDatasets = queryDatasetsIn(datasetResolver, lastPageQuery, ofSeconds(1));

        assertThat(lastPageDatasets).hasSize(100);
    }

    private Stream<Dataset> queryDatasetsIn(DatasetResolver datasetResolver, QuerySpec querySpec, Duration duration) {
        var start = clock.instant();
        var datasets = datasetResolver.query(new ParticipantAgent(emptyMap(), emptyMap()), querySpec);
        var end = clock.instant();

        assertThat(Duration.between(start, end)).isLessThan(duration);
        return datasets;
    }

    private ContractDefinition.Builder createContractDefinition(String id) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId("access")
                .contractPolicyId("contract");
    }

    @NotNull
    private AssetEntry createAssetEntry(Asset it) {
        return new AssetEntry(it, DataAddress.Builder.newInstance().type("type").build());
    }

    private Asset.Builder createAsset(String id) {
        return Asset.Builder.newInstance().id(id).name("test asset " + id);
    }
}

/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.service.catalog;

import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.spi.catalog.DatasetResolver;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatasetResolverImplTest {

    private final ContractDefinitionResolver contractDefinitionResolver = mock(ContractDefinitionResolver.class);
    private final AssetIndex assetIndex = mock(AssetIndex.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);

    private DatasetResolver datasetResolver;

    @BeforeEach
    void setUp() {
        datasetResolver = new DatasetResolverImpl(contractDefinitionResolver, assetIndex, policyStore);
    }

    @Test
    void query_shouldReturnOneDatasetPerAsset() {
        var dataService = createDataService();
        var contractDefinition = contractDefinitionBuilder("definitionId").contractPolicyId("contractPolicyId").build();
        var contractPolicy = Policy.Builder.newInstance().build();
        when(contractDefinitionResolver.definitionsFor(any())).thenReturn(Stream.of(contractDefinition));
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(createAsset("id").property("key", "value").build()));
        when(policyStore.findById("contractPolicyId")).thenReturn(PolicyDefinition.Builder.newInstance().policy(contractPolicy).build());

        var datasets = datasetResolver.query(createParticipantAgent(), QuerySpec.none(), dataService);

        assertThat(datasets).isNotNull().hasSize(1).first().satisfies(dataset -> {
            assertThat(dataset.getId()).matches(isUuid());
            assertThat(dataset.getDistributions()).hasSize(1).first().satisfies(distribution -> {
                assertThat(distribution.getDataService()).isSameAs(dataService);
            });
            assertThat(dataset.getOffers()).hasSize(1).allSatisfy((id, policy) -> {
                assertThat(id).startsWith("definitionId");
                assertThat(ContractId.parse(id)).matches(ContractId::isValid);
                assertThat(policy).isSameAs(contractPolicy);
            });
            assertThat(dataset.getProperties()).contains(entry("key", "value"));
        });
    }

    @Test
    void query_shouldReturnNoDataset_whenPolicyNotFound() {
        var dataService = createDataService();
        var contractDefinition = contractDefinitionBuilder("definitionId").contractPolicyId("contractPolicyId").build();
        when(contractDefinitionResolver.definitionsFor(any())).thenReturn(Stream.of(contractDefinition));
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(createAsset("id").build()));
        when(policyStore.findById("contractPolicyId")).thenReturn(null);

        var datasets = datasetResolver.query(createParticipantAgent(), QuerySpec.none(), dataService);

        assertThat(datasets).isNotNull().isEmpty();
    }

    @Test
    void query_shouldReturnOneDataset_whenMultipleDefinitionsOnSameAsset() {
        var dataService = createDataService();
        var policy1 = Policy.Builder.newInstance().build();
        var policy2 = Policy.Builder.newInstance().build();
        when(contractDefinitionResolver.definitionsFor(any())).thenReturn(Stream.of(
                contractDefinitionBuilder("definition1").contractPolicyId("policy1").build(),
                contractDefinitionBuilder("definition2").contractPolicyId("policy2").build()
        ));
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(i -> Stream.of(createAsset("id").build()));
        when(policyStore.findById("policy1")).thenReturn(PolicyDefinition.Builder.newInstance().policy(policy1).build());
        when(policyStore.findById("policy2")).thenReturn(PolicyDefinition.Builder.newInstance().policy(policy2).build());

        var datasets = datasetResolver.query(createParticipantAgent(), QuerySpec.none(), dataService);

        assertThat(datasets).hasSize(1).first().satisfies(dataset -> {
            assertThat(dataset.getId()).matches(isUuid());
            assertThat(dataset.getOffers()).hasSize(2)
                    .anySatisfy((id, policy) -> {
                        assertThat(id).startsWith("definition1");
                        assertThat(policy).isSameAs(policy1);
                    })
                    .anySatisfy((id, policy) -> {
                        assertThat(id).startsWith("definition2");
                        assertThat(policy).isSameAs(policy2);
                    });
        });
    }

    private ContractDefinition.Builder contractDefinitionBuilder(String id) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .validity(TimeUnit.MINUTES.toSeconds(10));
    }

    private Asset.Builder createAsset(String id) {
        return Asset.Builder.newInstance().id(id).name("test asset " + id);
    }

    private ParticipantAgent createParticipantAgent() {
        return new ParticipantAgent(emptyMap(), emptyMap());
    }

    private DataService createDataService() {
        return DataService.Builder.newInstance().build();
    }

    @NotNull
    private Predicate<String> isUuid() {
        return it -> {
            try {
                UUID.fromString(it);
                return true;
            } catch (Exception e) {
                return false;
            }
        };
    }

}

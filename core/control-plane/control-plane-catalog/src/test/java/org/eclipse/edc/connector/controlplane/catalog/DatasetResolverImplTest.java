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

package org.eclipse.edc.connector.controlplane.catalog;

import org.assertj.core.api.iterable.ThrowingExtractor;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.ContractDefinitionResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.ResolvedContractDefinitions;
import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.policy.model.PolicyType.OFFER;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetResolverImplTest {

    private final ContractDefinitionResolver definitionResolver = mock(ContractDefinitionResolver.class);
    private final AssetIndex assetIndex = mock(AssetIndex.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final DistributionResolver distributionResolver = mock(DistributionResolver.class);

    private DatasetResolver datasetResolver;

    @BeforeEach
    void setUp() {
        datasetResolver = new DatasetResolverImpl(definitionResolver, assetIndex, policyStore, distributionResolver,
                CriterionOperatorRegistryImpl.ofDefaults());
    }

    private ContractDefinition.Builder contractDefinitionBuilder(String id) {
        return ContractDefinition.Builder.newInstance()
                       .id(id)
                       .accessPolicyId("access")
                       .contractPolicyId("contract");
    }

    private Asset.Builder createAsset(String id) {
        return Asset.Builder.newInstance().id(id).name("test asset " + id);
    }

    private ParticipantAgent createParticipantAgent() {
        return new ParticipantAgent(emptyMap(), emptyMap());
    }

    private ParticipantContext createParticipantContext() {
        return ParticipantContext.Builder.newInstance().participantContextId("participantContextId").build();
    }

    private DataService createDataService() {
        return DataService.Builder.newInstance().build();
    }

    @NotNull
    private ThrowingExtractor<Dataset, Object, RuntimeException> getId() {
        return it -> it.getProperty(Asset.PROPERTY_ID);
    }

    @Nested
    class Query {
        @Test
        void shouldReturnOneDatasetPerAsset() {
            var dataService = createDataService();
            var contractDefinition = contractDefinitionBuilder("definitionId").contractPolicyId("contractPolicyId").build();
            var contractPolicy = Policy.Builder.newInstance().build();
            var distribution = Distribution.Builder.newInstance().dataService(dataService).format("format").build();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition)));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(createAsset("assetId").property("key", "value").build()));
            when(policyStore.findById("contractPolicyId")).thenReturn(PolicyDefinition.Builder.newInstance().policy(contractPolicy).build());
            when(distributionResolver.getDistributions(any(), isA(Asset.class))).thenReturn(List.of(distribution));

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), QuerySpec.none(), "protocol");

            assertThat(datasets).isNotNull().hasSize(1).first().satisfies(dataset -> {
                assertThat(dataset.getId()).isEqualTo("assetId");
                assertThat(dataset.getDistributions()).hasSize(1).first().isEqualTo(distribution);
                assertThat(dataset.getOffers()).hasSize(1).allSatisfy((id, policy) -> {
                    assertThat(ContractOfferId.parseId(id)).isSucceeded().extracting(ContractOfferId::definitionPart).asString().isEqualTo("definitionId");
                    assertThat(policy.getType()).isEqualTo(OFFER);
                    assertThat(policy.getTarget()).isEqualTo(null);
                });
                assertThat(dataset.getProperties()).contains(entry("key", "value"));
            });
        }

        @Test
        void shouldNotQueryAssets_whenNoValidContractDefinition() {
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(emptyList()));

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), QuerySpec.none(), "protocol");

            assertThat(datasets).isNotNull().isEmpty();
            verify(assetIndex, never()).queryAssets(any());
        }

        @Test
        void shouldReturnNoDataset_whenPolicyNotFound() {
            var contractDefinition = contractDefinitionBuilder("definitionId").contractPolicyId("contractPolicyId").build();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition)));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(createAsset("id").build()));
            when(policyStore.findById("contractPolicyId")).thenReturn(null);

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), QuerySpec.none(), "protocol");

            assertThat(datasets).isNotNull().isEmpty();
        }

        @Test
        void shouldReturnOneDataset_whenMultipleDefinitionsOnSameAsset() {
            var policy1 = Policy.Builder.newInstance().inheritsFrom("inherits1").build();
            var policy2 = Policy.Builder.newInstance().inheritsFrom("inherits2").build();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(
                    contractDefinitionBuilder("definition1").contractPolicyId("policy1").build(),
                    contractDefinitionBuilder("definition2").contractPolicyId("policy2").build()
            )));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(i -> Stream.of(createAsset("assetId").build()));
            when(policyStore.findById("policy1")).thenReturn(PolicyDefinition.Builder.newInstance().policy(policy1).build());
            when(policyStore.findById("policy2")).thenReturn(PolicyDefinition.Builder.newInstance().policy(policy2).build());

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), QuerySpec.none(), "protocol");

            assertThat(datasets).hasSize(1).first().satisfies(dataset -> {
                assertThat(dataset.getId()).isEqualTo("assetId");
                assertThat(dataset.getOffers()).hasSize(2)
                        .anySatisfy((id, policy) -> {
                            assertThat(ContractOfferId.parseId(id)).isSucceeded().extracting(ContractOfferId::definitionPart).asString().isEqualTo("definition1");
                            assertThat(policy.getInheritsFrom()).isEqualTo("inherits1");
                        })
                        .anySatisfy((id, policy) -> {
                            assertThat(ContractOfferId.parseId(id)).isSucceeded().extracting(ContractOfferId::definitionPart).asString().isEqualTo("definition2");
                            assertThat(policy.getInheritsFrom()).isEqualTo("inherits2");
                        });
            });
        }

        @Test
        void shouldFilterAssetsByPassedCriteria() {
            var definitionCriterion = new Criterion(EDC_NAMESPACE + "id", "=", "id");
            var contractDefinition = contractDefinitionBuilder("definitionId")
                                             .assetsSelector(List.of(definitionCriterion))
                                             .contractPolicyId("contractPolicyId")
                                             .build();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition)));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(createAsset("id").property("key", "value").build()));
            when(policyStore.findById("contractPolicyId")).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());
            var additionalCriterion = new Criterion(EDC_NAMESPACE + "key", "=", "value");
            var querySpec = QuerySpec.Builder.newInstance().filter(additionalCriterion).build();

            datasetResolver.query(createParticipantContext(), createParticipantAgent(), querySpec, "protocol");

            verify(assetIndex).queryAssets(and(
                    isA(QuerySpec.class),
                    argThat(q -> q.getFilterExpression().contains(additionalCriterion))
            ));
        }

        @Test
        void shouldLimitDataset_whenSingleDefinitionAndMultipleAssets_contained() {
            var contractDefinition = contractDefinitionBuilder("definitionId").contractPolicyId("contractPolicyId").build();
            var contractPolicy = Policy.Builder.newInstance().build();
            var assets = range(0, 10).mapToObj(it -> createAsset(String.valueOf(it)).build()).toList();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition)));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(i -> assets.stream());
            when(policyStore.findById("contractPolicyId")).thenReturn(PolicyDefinition.Builder.newInstance().policy(contractPolicy).build());
            var querySpec = QuerySpec.Builder.newInstance().range(new Range(2, 5)).build();

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), querySpec, "protocol");

            assertThat(datasets).hasSize(3).map(getId()).containsExactly("2", "3", "4");
        }

        @Test
        void shouldLimitDataset_whenSingleDefinitionAndMultipleAssets_overflowing() {
            var contractDefinition = contractDefinitionBuilder("definitionId").contractPolicyId("contractPolicyId").build();
            var contractPolicy = Policy.Builder.newInstance().build();
            var assets = range(0, 10).mapToObj(it -> createAsset(String.valueOf(it)).build()).toList();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition)));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(i -> assets.stream());
            when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(contractPolicy).build());
            var querySpec = QuerySpec.Builder.newInstance().range(new Range(7, 15)).build();

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), querySpec, "protocol");

            assertThat(datasets).hasSize(3).map(getId()).containsExactly("7", "8", "9");
        }

        @Test
        void shouldLimitDataset_whenMultipleDefinitionAndMultipleAssets_across() {
            var contractDefinitions = range(0, 2).mapToObj(it -> contractDefinitionBuilder(String.valueOf(it)).build()).toList();
            var contractPolicy = Policy.Builder.newInstance().build();
            var assets = range(0, 20).mapToObj(it -> createAsset(String.valueOf(it)).build()).toList();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(contractDefinitions));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(i -> assets.stream());
            when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(contractPolicy).build());
            var querySpec = QuerySpec.Builder.newInstance().range(new Range(6, 14)).build();

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), querySpec, "protocol");

            assertThat(datasets).hasSize(8).map(getId()).containsExactly("6", "7", "8", "9", "10", "11", "12", "13");
        }

        @Test
        void shouldLimitDataset_whenMultipleDefinitionsWithSameAssets() {
            var contractDefinitions = range(0, 2).mapToObj(it -> contractDefinitionBuilder(String.valueOf(it)).build()).toList();
            var contractPolicy = Policy.Builder.newInstance().build();
            var assets = range(0, 10).mapToObj(it -> createAsset(String.valueOf(it)).build()).toList();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(contractDefinitions));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(i -> assets.stream());
            when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(contractPolicy).build());
            var querySpec = QuerySpec.Builder.newInstance().range(new Range(6, 8)).build();

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), querySpec, "protocol");

            assertThat(datasets).hasSize(2)
                    .allSatisfy(dataset -> assertThat(dataset.getOffers()).hasSize(2))
                    .map(getId()).containsExactly("6", "7");
        }

        @Test
        void shouldReturnCatalogWithinCatalog_whenAssetIsCatalogAsset() {
            var contractDefinition = contractDefinitionBuilder("definitionId").contractPolicyId("contractPolicyId").build();
            var contractPolicy = Policy.Builder.newInstance().build();
            var distribution = Distribution.Builder.newInstance().dataService(DataService.Builder.newInstance()
                                                                                      .endpointDescription("test-asset-desc")
                                                                                      .endpointUrl("https://foo.bar/baz")
                                                                                      .build())
                                       .format(HttpDataAddressSchema.HTTP_DATA_TYPE).build();

            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition)));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(createAsset("assetId")
                                                                                            .property(Asset.PROPERTY_IS_CATALOG, true)
                                                                                            .dataAddress(DataAddress.Builder.newInstance().type(HttpDataAddressSchema.HTTP_DATA_TYPE).build())
                                                                                            .build()));
            when(policyStore.findById("contractPolicyId")).thenReturn(PolicyDefinition.Builder.newInstance().policy(contractPolicy).build());
            when(distributionResolver.getDistributions(any(), isA(Asset.class))).thenReturn(List.of(distribution));

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), QuerySpec.none(), "protocol");

            assertThat(datasets).isNotNull().hasSize(1).first().satisfies(dataset -> {
                assertThat(dataset).isInstanceOf(Catalog.class);
                assertThat(dataset.getId()).isEqualTo("assetId");
                assertThat(dataset.getOffers()).hasSize(1).allSatisfy((id, policy) -> {
                    assertThat(ContractOfferId.parseId(id)).isSucceeded().extracting(ContractOfferId::definitionPart).asString().isEqualTo("definitionId");
                    assertThat(policy.getType()).isEqualTo(OFFER);
                    assertThat(policy.getTarget()).isEqualTo(null);
                });
            });
        }

        @Test
        void shouldNotFetchContractPolicy_whenIsSameAsAccessPolicy() {
            var dataService = createDataService();
            var contractDefinition = contractDefinitionBuilder("definitionId").accessPolicyId("samePolicy").contractPolicyId("samePolicy").build();
            var distribution = Distribution.Builder.newInstance().dataService(dataService).format("format").build();
            var cachedPolicies = new HashMap<>(Map.of("samePolicy", Policy.Builder.newInstance().build()));
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition), cachedPolicies));
            when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(createAsset("assetId").property("key", "value").build()));
            when(distributionResolver.getDistributions(any(), isA(Asset.class))).thenReturn(List.of(distribution));

            var datasets = datasetResolver.query(createParticipantContext(), createParticipantAgent(), QuerySpec.none(), "protocol");

            assertThat(datasets).hasSize(1);
            verify(policyStore, never()).findById(any());
        }
    }

    @Nested
    class GetById {
        @Test
        void shouldReturnDataset() {
            var policy1 = Policy.Builder.newInstance().inheritsFrom("inherits1").build();
            var policy2 = Policy.Builder.newInstance().inheritsFrom("inherits2").build();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(
                    contractDefinitionBuilder("definition1").contractPolicyId("policy1").build(),
                    contractDefinitionBuilder("definition2").contractPolicyId("policy2").build()
            )));
            when(assetIndex.findById(any())).thenReturn(createAsset("datasetId").build());
            when(policyStore.findById("policy1")).thenReturn(PolicyDefinition.Builder.newInstance().policy(policy1).build());
            when(policyStore.findById("policy2")).thenReturn(PolicyDefinition.Builder.newInstance().policy(policy2).build());
            var participantAgent = createParticipantAgent();

            var dataset = datasetResolver.getById(createParticipantContext(), participantAgent, "datasetId", "protocol");

            assertThat(dataset).isNotNull();
            assertThat(dataset.getId()).isEqualTo("datasetId");
            assertThat(dataset.getOffers()).hasSize(2)
                    .anySatisfy((id, policy) -> {
                        assertThat(ContractOfferId.parseId(id)).isSucceeded().extracting(ContractOfferId::definitionPart).isEqualTo("definition1");
                        assertThat(policy.getInheritsFrom()).isEqualTo("inherits1");
                    })
                    .anySatisfy((id, policy) -> {
                        assertThat(ContractOfferId.parseId(id)).isSucceeded().extracting(ContractOfferId::definitionPart).isEqualTo("definition2");
                        assertThat(policy.getInheritsFrom()).isEqualTo("inherits2");
                    });
            verify(assetIndex).findById("datasetId");
            verify(definitionResolver).resolveFor(argThat(argument -> argument.getParticipantContextId().equals("participantContextId")), eq(participantAgent));
        }

        @Test
        void shouldReturnNull_whenAssetNotFound() {
            var contractDefinition = contractDefinitionBuilder("definition1").contractPolicyId("policy1").build();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition)));
            when(assetIndex.findById(any())).thenReturn(null);
            var participantAgent = createParticipantAgent();

            var dataset = datasetResolver.getById(createParticipantContext(), participantAgent, "datasetId", "protocol");

            assertThat(dataset).isNull();
        }

        @Test
        void shouldReturnNull_whenNoValidContractDefinition() {
            var participantAgent = createParticipantAgent();

            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(emptyList()));

            var dataset = datasetResolver.getById(createParticipantContext(), participantAgent, "datasetId", "protocol");

            assertThat(dataset).isNull();
            verify(assetIndex, never()).findById(any());
        }

        @Test
        void shouldReturnNull_whenNoValidContractDefinitionForAsset() {
            var assetId = "assetId";
            var participantAgent = createParticipantAgent();

            var contractDefinition = contractDefinitionBuilder("definition")
                                             .assetsSelectorCriterion(Criterion.Builder.newInstance()
                                                                              .operandRight(EDC_NAMESPACE + "id")
                                                                              .operator("=")
                                                                              .operandLeft("a-different-asset")
                                                                              .build())
                                             .build();
            when(definitionResolver.resolveFor(any(), any())).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition)));
            when(assetIndex.findById(any())).thenReturn(createAsset(assetId).build());

            var dataset = datasetResolver.getById(createParticipantContext(), participantAgent, assetId, "protocol");

            assertThat(dataset).isNull();
        }
    }

}

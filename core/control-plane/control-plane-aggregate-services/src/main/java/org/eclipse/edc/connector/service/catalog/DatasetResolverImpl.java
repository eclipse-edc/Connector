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

package org.eclipse.edc.connector.service.catalog;

import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.spi.catalog.DatasetResolver;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class DatasetResolverImpl implements DatasetResolver {

    private final ContractDefinitionResolver contractDefinitionResolver;
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyDefinitionStore;

    public DatasetResolverImpl(ContractDefinitionResolver contractDefinitionResolver, AssetIndex assetIndex, PolicyDefinitionStore policyDefinitionStore) {
        this.contractDefinitionResolver = contractDefinitionResolver;
        this.assetIndex = assetIndex;
        this.policyDefinitionStore = policyDefinitionStore;
    }

    @Override
    @NotNull
    public Stream<Dataset> query(ParticipantAgent agent, QuerySpec querySpec, DataService dataService) {
        return contractDefinitionResolver
                .definitionsFor(agent)
                .flatMap(definition -> {
                    var definitionCriteria = definition.getSelectorExpression().getCriteria();
                    var filterCriteria = querySpec.getFilterExpression();
                    var assetQuery = QuerySpec.Builder.newInstance().filter(concat(definitionCriteria, filterCriteria)).build();
                    return assetIndex.queryAssets(assetQuery)
                            .map(asset -> {
                                var offer = createOffer(definition);
                                if (offer == null) {
                                    return null;
                                }
                                return entry(asset.getProperties(), offer);
                            })
                            .filter(Objects::nonNull);
                })
                .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toSet())))
                .entrySet().stream()
                .filter(it -> !it.getValue().isEmpty())
                .map(entry -> {
                    var datasetBuilder = Dataset.Builder.newInstance()
                            .distribution(createDistribution(dataService))
                            .properties(entry.getKey());

                    entry.getValue().forEach(offer -> datasetBuilder.offer(offer.contractId, offer.policy));

                    return datasetBuilder.build();
                });
    }

    @NotNull
    private List<Criterion> concat(List<Criterion> list1, List<Criterion> list2) {
        return Stream.concat(list1.stream(), list2.stream()).collect(toList());
    }

    private Distribution createDistribution(DataService dataService) {
        return Distribution.Builder.newInstance()
                .dataService(dataService)
                .format("any") // TODO: should it represent the allowedDestTypes of the associated dataplanes?
                .build();
    }

    private Offer createOffer(ContractDefinition definition) {
        var policyDefinition = policyDefinitionStore.findById(definition.getContractPolicyId());
        if (policyDefinition == null) {
            return null;
        }
        var contractId = ContractId.createContractId(definition.getId());
        return new Offer(contractId, policyDefinition.getPolicy());
    }

    private static class Offer {
        private final String contractId;
        private final Policy policy;

        Offer(String contractId, Policy policy) {
            this.contractId = contractId;
            this.policy = policy;
        }
    }

}

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
import org.eclipse.edc.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetPredicateConverter;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static java.util.stream.Collectors.toList;

public class DatasetResolverImpl implements DatasetResolver {

    private final ContractDefinitionResolver contractDefinitionResolver;
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyDefinitionStore;
    private final DistributionResolver distributionResolver;
    private final AssetPredicateConverter predicateConverter = new AssetPredicateConverter();

    public DatasetResolverImpl(ContractDefinitionResolver contractDefinitionResolver, AssetIndex assetIndex,
                               PolicyDefinitionStore policyDefinitionStore, DistributionResolver distributionResolver) {
        this.contractDefinitionResolver = contractDefinitionResolver;
        this.assetIndex = assetIndex;
        this.policyDefinitionStore = policyDefinitionStore;
        this.distributionResolver = distributionResolver;
    }

    @Override
    @NotNull
    public Stream<Dataset> query(ParticipantAgent agent, QuerySpec querySpec) {
        var contractDefinitions = contractDefinitionResolver.definitionsFor(agent).collect(toList());
        var assetsQuery = QuerySpec.Builder.newInstance().offset(0).limit(MAX_VALUE).filter(querySpec.getFilterExpression()).build();
        return assetIndex.queryAssets(assetsQuery)
                .map(asset -> {
                    var offers = contractDefinitions.stream()
                            .filter(definition -> definition.getAssetsSelector().stream()
                                    .map(predicateConverter::convert)
                                    .reduce(x -> true, Predicate::and)
                                    .test(asset))
                            .map(contractDefinition -> createOffer(contractDefinition, asset.getId()))
                            .filter(Objects::nonNull)
                            .collect(toList());
                    return new ProtoDataset(asset, offers);
                })
                .filter(ProtoDataset::hasOffers)
                .skip(querySpec.getOffset())
                .limit(querySpec.getLimit())
                .map(proto -> {
                    var asset = proto.asset;
                    var offers = proto.offers;
                    var distributions = distributionResolver.getDistributions(asset, null); // TODO: data addresses should be retrieved
                    var datasetBuilder = Dataset.Builder.newInstance()
                            .distributions(distributions)
                            .properties(asset.getProperties());

                    offers.forEach(offer -> datasetBuilder.offer(offer.contractId, offer.policy.withTarget(asset.getId())));

                    return datasetBuilder.build();
                });
    }

    private Offer createOffer(ContractDefinition definition, String assetId) {
        var policyDefinition = policyDefinitionStore.findById(definition.getContractPolicyId());
        if (policyDefinition == null) {
            return null;
        }
        var contractId = ContractId.create(definition.getId(), assetId);
        return new Offer(contractId.toString(), policyDefinition.getPolicy());
    }

    private static class Offer {
        private final String contractId;
        private final Policy policy;

        Offer(String contractId, Policy policy) {
            this.contractId = contractId;
            this.policy = policy;
        }
    }

    private static class ProtoDataset {
        private final Asset asset;
        private final List<Offer> offers;

        private ProtoDataset(Asset asset, List<Offer> offers) {
            this.asset = asset;
            this.offers = offers;
        }

        boolean hasOffers() {
            return offers.size() > 0;
        }
    }

}

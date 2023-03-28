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

package org.eclipse.edc.connector.contract.offer;

import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionService;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.DatasetResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.DataService;
import org.eclipse.edc.connector.contract.spi.types.offer.Dataset;
import org.eclipse.edc.connector.contract.spi.types.offer.Distribution;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;

public class DatasetResolverImpl implements DatasetResolver {
    
    private final ParticipantAgentService agentService;
    private final ContractDefinitionService definitionService;
    private final AssetIndex assetIndex;
    private final DataAddressResolver dataAddressResolver;
    private final PolicyDefinitionStore policyStore;
    private final Monitor monitor;
    
    public DatasetResolverImpl(ParticipantAgentService agentService, ContractDefinitionService definitionService,
                               AssetIndex assetIndex, DataAddressResolver dataAddressResolver,
                               PolicyDefinitionStore policyStore, Monitor monitor) {
        this.agentService = agentService;
        this.definitionService = definitionService;
        this.assetIndex = assetIndex;
        this.dataAddressResolver = dataAddressResolver;
        this.policyStore = policyStore;
        this.monitor = monitor;
    }
    
    @Override
    public @NotNull Stream<Dataset> queryDatasets(ContractOfferQuery query) {
        var agent = agentService.createFor(query.getClaimToken());
        
        var assetQuerySpec = QuerySpec.Builder.newInstance()
                .range(query.getRange())
                .filter(query.getAssetsCriteria())
                .build();
        
        var distributions = new ArrayList<Distribution>();
        var dataService = createDataService();
    
        var definitions = definitionService.definitionsFor(agent).collect(Collectors.toList());
        return assetIndex.queryAssets(assetQuerySpec)
                .flatMap(asset -> {
                    var datasetBuilder = createDataset(asset, distributions, dataService);
                    
                    definitions.stream()
                            .filter(definition -> assetMatchesDefinition(asset, definition))
                            .forEach(definition -> {
                                var offerId = ContractId.createContractId(definition.getId());
                                var policyDefinition = policyStore.findById(definition.getContractPolicyId());
                                if (policyDefinition != null) {
                                    datasetBuilder.offer(offerId, policyDefinition.getPolicy());
                                }
                            });
                    
                    var dataset = datasetBuilder.build();
                    
                    if (dataset.getOffers() != null && !dataset.getOffers().isEmpty()) {
                        return Stream.of(dataset);
                    } else {
                        return Stream.empty();
                    }
                });
    }
    
    private DataService createDataService() {
        return DataService.Builder.newInstance()
                .id(randomUUID().toString())
                .terms("dspace:connector")
                .endpointUrl("https://localhost:8282") //TODO get connector address
                .build();
    }
    
    private Dataset.Builder createDataset(Asset asset, List<Distribution> distributions, DataService dataService) {
       return Dataset.Builder.newInstance()
                .id(randomUUID().toString())
                .properties(asset.getProperties())
                .distribution(createDistribution(asset, distributions, dataService));
    }

    private Distribution createDistribution(Asset asset, List<Distribution> distributions, DataService dataService) {
        var dataAddress = dataAddressResolver.resolveForAsset(asset.getId());
        var format = dataAddress.getType(); //TODO create format from type
        
        var existingDistribution = distributions.stream()
                .filter(d -> d.getFormat().equals(format))
                .findFirst()
                .orElse(null);

        if (existingDistribution != null) {
            return existingDistribution;
        } else {
            var distribution = Distribution.Builder.newInstance()
                    .format(dataAddress.getType())
                    .dataService(dataService)
                    .build();
            distributions.add(distribution);
            return distribution;
        }
    }
    
    private boolean assetMatchesDefinition(Asset asset, ContractDefinition contractDefinition) {
        return contractDefinition.getSelectorExpression().getCriteria().stream()
                .map(criterion -> new BaseCriterionToPredicateConverter<>() {
                    @Override
                    protected <R> R property(String key, Object object) {
                        var asset = (Asset) object;
                        return asset.getProperties() == null ? null : (R) asset.getProperty(key);
                    }
                }.convert(criterion))
                .reduce(x -> true, Predicate::and)
                .test(asset);
    }
}

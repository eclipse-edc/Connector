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

package org.eclipse.edc.protocol.dsp.catalog.service;

import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.DatasetResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.DataService;
import org.eclipse.edc.connector.contract.spi.types.offer.Distribution;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.catalog.service.CatalogService;
import org.eclipse.edc.protocol.dsp.spi.catalog.types.CatalogRequestMessage;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

public class CatalogServiceImpl implements CatalogService {
    
    private JsonLdTransformerRegistry transformerRegistry;
    private DatasetResolver datasetResolver;
    
    public CatalogServiceImpl(JsonLdTransformerRegistry transformerRegistry, DatasetResolver datasetResolver) {
        this.transformerRegistry = transformerRegistry;
        this.datasetResolver = datasetResolver;
    }
    
    @Override
    public JsonObject getCatalog(JsonObject request, ClaimToken claimToken) {
        var result = transformerRegistry.transform(request, CatalogRequestMessage.class);
        if (result.failed()) {
            throw new InvalidRequestException("Request body was malformed.");
        }
        
        var querySpec = result.getContent().getFilter();
        var query = ContractOfferQuery.Builder.newInstance()
                .range(querySpec.getRange())
                .assetsCriteria(querySpec.getFilterExpression())
                .claimToken(claimToken)
                .build();
        
        var datasets = datasetResolver.queryDatasets(query).collect(Collectors.toList());
        
        var dataServices = new HashSet<DataService>();
        for (var dataset : datasets) {
            dataset.getDistributions().stream()
                    .map(Distribution::getDataService)
                    .forEach(dataServices::add);
        }
        
        var catalog = Catalog.Builder.newInstance()
                .id(randomUUID().toString())
                .datasets(datasets)
                .dataServices(new ArrayList<>(dataServices))
                .build();
    
        var catalogResult = transformerRegistry.transform(catalog, JsonObject.class);
        if (catalogResult.succeeded()) {
            return catalogResult.getContent();
        } else {
            throw new EdcException("Response could not be created.");
        }
    }
}

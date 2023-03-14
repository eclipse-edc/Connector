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

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.DatasetResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.DataService;
import org.eclipse.edc.connector.contract.spi.types.offer.Distribution;
import org.eclipse.edc.protocol.dsp.spi.catalog.service.CatalogService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

public class CatalogServiceImpl implements CatalogService {
    
    private DatasetResolver datasetResolver;
    
    public CatalogServiceImpl(DatasetResolver datasetResolver) {
        this.datasetResolver = datasetResolver;
    }
    
    @Override
    public Catalog getCatalog(ContractOfferQuery query) {
        var datasets = datasetResolver.queryDatasets(query).collect(Collectors.toList());

        var dataServices = new HashSet<DataService>();
        for (var dataset : datasets) {
            dataset.getDistributions().stream()
                    .map(Distribution::getDataService)
                    .forEach(dataServices::add);
        }
        
        return Catalog.Builder.newInstance()
                .id(randomUUID().toString())
                .datasets(datasets)
                .dataServices(new ArrayList<>(dataServices))
                .build();
    }
}

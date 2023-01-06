/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
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
 *       ZF Friedrichshafen AG - enable asset filtering
 *
 */

package org.eclipse.edc.protocol.ids.service;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.protocol.ids.spi.service.CatalogService;
import org.eclipse.edc.protocol.ids.spi.types.container.DescriptionRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class CatalogServiceImpl implements CatalogService {
    private final String dataCatalogId;
    private final ContractOfferResolver contractOfferResolver;

    public CatalogServiceImpl(
            @NotNull String dataCatalogId,
            @NotNull ContractOfferResolver contractOfferResolver) {
        this.dataCatalogId = Objects.requireNonNull(dataCatalogId);
        this.contractOfferResolver = Objects.requireNonNull(contractOfferResolver);
    }

    @Override
    public @NotNull Catalog getDataCatalog(@NotNull DescriptionRequest descriptionRequest) {
        var querySpec = descriptionRequest.getQuerySpec();

        var query = ContractOfferQuery.Builder.newInstance()
                .claimToken(descriptionRequest.getClaimToken())
                .assetsCriteria(querySpec.getFilterExpression())
                .range(querySpec.getRange())
                .provider(descriptionRequest.getProvider())
                .consumer(descriptionRequest.getConsumer())
                .build();

        try (var offers = contractOfferResolver.queryContractOffers(query)) {
            return Catalog.Builder.newInstance()
                    .id(dataCatalogId)
                    .contractOffers(offers.collect(toList()))
                    .build();
        }
    }
}

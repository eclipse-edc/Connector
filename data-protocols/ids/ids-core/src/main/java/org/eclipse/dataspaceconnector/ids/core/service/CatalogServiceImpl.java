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

package org.eclipse.dataspaceconnector.ids.core.service;

import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferResolver;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
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

    /**
     * Provides the dataCatalog object, which may be used by the IDS self-description of the connector.
     *
     * @return data catalog
     */
    @Override
    @NotNull
    public Catalog getDataCatalog(ClaimToken claimToken, QuerySpec querySpec) {

        var query = ContractOfferQuery.Builder.newInstance()
                .claimToken(claimToken)
                .assetsCriteria(querySpec.getFilterExpression())
                .range(querySpec.getRange()).build();

        try (var offers = contractOfferResolver.queryContractOffers(query)) {
            return Catalog.Builder.newInstance()
                    .id(dataCatalogId)
                    .contractOffers(offers.collect(toList()))
                    .build();
        }
    }
}

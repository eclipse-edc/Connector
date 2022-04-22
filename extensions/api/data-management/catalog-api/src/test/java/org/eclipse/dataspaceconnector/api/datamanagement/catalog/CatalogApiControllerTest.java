/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.catalog;

import com.github.javafaker.Faker;
import jakarta.ws.rs.container.AsyncResponse;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogService;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogApiControllerTest {

    private static final Faker FAKER = Faker.instance();
    private final CatalogService service = mock(CatalogService.class);

    @Test
    void shouldGetTheCatalog() {
        var controller = new CatalogApiController(service);
        var response = mock(AsyncResponse.class);
        var offer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policyId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .build();
        var catalog = Catalog.Builder.newInstance().id("any").contractOffers(List.of(offer)).build();
        var url = FAKER.internet().url();
        when(service.getByProviderUrl(url)).thenReturn(completedFuture(catalog));

        controller.getCatalog(url, response);

        verify(response).resume(Mockito.<Catalog>argThat(c -> c.getContractOffers().equals(List.of(offer))));
    }

    @Test
    void shouldResumeWithExceptionIfGetCatalogFails() {
        var controller = new CatalogApiController(service);
        var response = mock(AsyncResponse.class);
        var url = FAKER.internet().url();
        when(service.getByProviderUrl(url)).thenReturn(failedFuture(new EdcException("error")));

        controller.getCatalog(url, response);

        verify(response).resume(isA(EdcException.class));
    }
}

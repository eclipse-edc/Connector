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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogService;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        var headers = mock(HttpHeaders.class);
        var headersMap = new MultivaluedHashMap<String, String>();
        var offer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString())
                .build();
        var catalog = Catalog.Builder.newInstance().id("any").contractOffers(List.of(offer)).build();
        var url = FAKER.internet().url();

        headersMap.put("property_partition", Collections.singletonList("part1"));
        headersMap.put("some header", Collections.singletonList("header val"));

        when(headers.getRequestHeaders()).thenReturn(headersMap);
        when(service.getByProviderUrl(url, Map.of("partition", "part1"))).thenReturn(completedFuture(catalog));

        controller.getCatalog(url, headers, response);

        verify(response).resume(Mockito.<Catalog>argThat(c -> c.getContractOffers().equals(List.of(offer))));
    }

    @Test
    void shouldResumeWithExceptionIfGetCatalogFails() {
        var controller = new CatalogApiController(service);
        var response = mock(AsyncResponse.class);
        var url = FAKER.internet().url();
        var headers = mock(HttpHeaders.class);
        var headersMap = new MultivaluedHashMap<String, String>();

        when(headers.getRequestHeaders()).thenReturn(headersMap);
        when(service.getByProviderUrl(url, Map.of())).thenReturn(failedFuture(new EdcException("error")));

        controller.getCatalog(url, headers, response);

        verify(response).resume(isA(EdcException.class));
    }
}

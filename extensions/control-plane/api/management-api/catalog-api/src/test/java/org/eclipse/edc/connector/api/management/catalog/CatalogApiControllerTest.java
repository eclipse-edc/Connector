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

package org.eclipse.edc.connector.api.management.catalog;

import jakarta.ws.rs.container.AsyncResponse;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogApiControllerTest {

    private final CatalogService service = mock(CatalogService.class);
    private final Monitor monitor = mock(Monitor.class);
    private TypeTransformerRegistry transformerRegistry;

    private static ContractOffer createContractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .assetId(randomUUID().toString())
                .build();
    }

    @BeforeEach
    void setup() {
        transformerRegistry = mock(TypeTransformerRegistry.class);
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(new QuerySpec()));
    }

    @Test
    void shouldGetTheCatalog() {
        var controller = new CatalogApiController(service, transformerRegistry, monitor);
        var response = mock(AsyncResponse.class);
        var offer = createContractOffer();
        var catalog = Catalog.Builder.newInstance().id("any").contractOffers(List.of(offer)).build();
        var url = "test.url";
        when(service.getByProviderUrl(eq(url), any())).thenReturn(completedFuture(catalog));

        controller.getCatalog(url, new QuerySpecDto(), response);

        verify(response).resume(Mockito.<Catalog>argThat(c -> c.getContractOffers().equals(List.of(offer))));
    }

    @Test
    void shouldResumeWithExceptionIfGetCatalogFails() {
        var controller = new CatalogApiController(service, transformerRegistry, monitor);
        var response = mock(AsyncResponse.class);
        var url = "test.url";
        when(service.getByProviderUrl(eq(url), any())).thenReturn(failedFuture(new EdcException("error")));

        controller.getCatalog(url, new QuerySpecDto(), response);

        verify(response).resume(isA(EdcException.class));
    }

    @Test
    void shouldPostCatalogRequest() {
        var controller = new CatalogApiController(service, transformerRegistry, monitor);
        var response = mock(AsyncResponse.class);
        var offer = createContractOffer();
        var catalog = Catalog.Builder.newInstance().id("any").contractOffers(List.of(offer)).build();
        var url = "test.url";

        when(service.getByProviderUrl(eq(url), any())).thenReturn(completedFuture(catalog));

        var request = CatalogRequestDto.Builder.newInstance()
                .providerUrl(url)
                .querySpec(QuerySpecDto.Builder.newInstance()
                        .sortField("test-field")
                        .sortOrder(SortOrder.DESC)
                        .filterExpression(List.of(TestFunctions.createCriterionDto("foo", "=", "bar")))
                        .build())
                .build();

        controller.requestCatalog(request, response);

        verify(response).resume(Mockito.<Catalog>argThat(c -> c.getContractOffers().equals(List.of(offer))));
    }
}

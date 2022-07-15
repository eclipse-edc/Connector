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
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogApiControllerTest {

    private static final Faker FAKER = Faker.instance();
    private final CatalogService service = mock(CatalogService.class);
    private final Monitor monitor = mock(Monitor.class);
    private DtoTransformerRegistry transformerRegistry;

    @BeforeEach
    void setup() {
        transformerRegistry = mock(DtoTransformerRegistry.class);
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(new QuerySpec()));
    }


    @Test
    void shouldGetTheCatalog() {
        var controller = new CatalogApiController(service, transformerRegistry, monitor);
        var response = mock(AsyncResponse.class);
        var offer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString())
                .build();
        var catalog = Catalog.Builder.newInstance().id("any").contractOffers(List.of(offer)).build();
        var url = FAKER.internet().url();
        when(service.getByProviderUrl(eq(url), any())).thenReturn(completedFuture(catalog));

        controller.getCatalog(url, new QuerySpecDto(), response);

        verify(response).resume(Mockito.<Catalog>argThat(c -> c.getContractOffers().equals(List.of(offer))));
    }

    @Test
    void shouldResumeWithExceptionIfGetCatalogFails() {
        var controller = new CatalogApiController(service, transformerRegistry, monitor);
        var response = mock(AsyncResponse.class);
        var url = FAKER.internet().url();
        when(service.getByProviderUrl(eq(url), any())).thenReturn(failedFuture(new EdcException("error")));

        controller.getCatalog(url, new QuerySpecDto(), response);

        verify(response).resume(isA(EdcException.class));
    }
}

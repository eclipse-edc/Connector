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

package org.eclipse.dataspaceconnector.api.datamanagement.catalog.service;

import com.github.javafaker.Faker;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogServiceImplTest {

    private static final Faker FAKER = Faker.instance();
    private final RemoteMessageDispatcherRegistry dispatcher = mock(RemoteMessageDispatcherRegistry.class);

    @Test
    void shouldSendCatalogRequestToDispatcher() {
        var service = new CatalogServiceImpl(dispatcher);
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .policyId(FAKER.internet().uuid())
                .assetId(FAKER.internet().uuid())
                .build();
        var catalog = Catalog.Builder.newInstance().id("id").contractOffers(List.of(contractOffer)).build();
        when(dispatcher.send(any(), any(), any())).thenReturn(completedFuture(catalog));

        var future = service.getByProviderUrl(FAKER.internet().url());

        assertThat(future).succeedsWithin(1, SECONDS).extracting(Catalog::getContractOffers, InstanceOfAssertFactories.list(ContractOffer.class)).hasSize(1);
        verify(dispatcher).send(eq(Catalog.class), isA(CatalogRequest.class), any());
    }
}

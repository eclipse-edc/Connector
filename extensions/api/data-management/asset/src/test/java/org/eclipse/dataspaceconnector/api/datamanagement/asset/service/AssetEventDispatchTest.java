/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset.service;

import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.event.Event;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.event.EventSubscriber;
import org.eclipse.dataspaceconnector.spi.event.asset.AssetCreated;
import org.eclipse.dataspaceconnector.spi.event.asset.AssetDeleted;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(EdcExtension.class)
public class AssetEventDispatchTest {

    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);

    @Test
    void shouldDispatchEventsOnAssetCreationAndDeletion(AssetService service, EventRouter eventRouter) throws InterruptedException {
        var createdLatch = onDispatchLatch(AssetCreated.class);
        var deletedLatch = onDispatchLatch(AssetDeleted.class);
        eventRouter.register(eventSubscriber);
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();

        service.create(asset, dataAddress);

        assertThat(createdLatch.await(10, SECONDS)).isTrue();
        verify(eventSubscriber).on(isA(AssetCreated.class));

        service.delete(asset.getId());

        assertThat(deletedLatch.await(10, SECONDS)).isTrue();
        verify(eventSubscriber).on(isA(AssetDeleted.class));
    }

    private CountDownLatch onDispatchLatch(Class<? extends Event> eventType) {
        var latch = new CountDownLatch(1);

        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(eventSubscriber).on(isA(eventType));

        return latch;
    }

}

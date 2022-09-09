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
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.event.EventSubscriber;
import org.eclipse.dataspaceconnector.spi.event.asset.AssetCreated;
import org.eclipse.dataspaceconnector.spi.event.asset.AssetDeleted;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(EdcExtension.class)
public class AssetEventDispatchTest {

    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api"
        ));
    }

    @Test
    void shouldDispatchEventsOnAssetCreationAndDeletion(AssetService service, EventRouter eventRouter) throws InterruptedException {

        doAnswer(i -> null).when(eventSubscriber).on(isA(AssetCreated.class));
        doAnswer(i -> null).when(eventSubscriber).on(isA(AssetDeleted.class));

        eventRouter.register(eventSubscriber);
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();

        service.create(asset, dataAddress);
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(AssetCreated.class));
        });


        service.delete(asset.getId());
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(AssetDeleted.class));
        });
    }
}

/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.object;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.common.DataDestination;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.object.ObjectStorageObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


class DemoObjectStorageTest {

    private DemoObjectStorage objectStore;

    @Test
    void verifyStorage() throws Exception {
        ObjectStorageObserver observer = mock(ObjectStorageObserver.class);
        objectStore.register(observer);

        objectStore.start();
        var destination = objectStore.provision("test").get();

        assertEquals("test", destination.getDestinationName());

        objectStore.store("test", "data1", destination.getAccessToken(), "test".getBytes());

        objectStore.deprovision("test");

        verify(observer).onStore(isA(String.class), isA(String.class), isA(String.class), isA(byte[].class));
        verify(observer).onDeprovision(isA(String.class));
        verify(observer).onProvision(isA(DataDestination.class));
    }

    @BeforeEach
    void setUp() {
        objectStore = new DemoObjectStorage(new Monitor() {
        });
        objectStore.setProvisionWait(1);
    }

    @AfterEach
    void tearDown() {
        objectStore.stop();
    }
}

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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.stream;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DemoTopicManagerTest {
    private DemoTopicManager topicManager;

    @Test
    void verifyPubSub() throws Exception {
        topicManager.start();

        var dataDestination = topicManager.provision("destination").get();
        var cdl = new CountDownLatch(2);

        Consumer<byte[]> consumer1 = mock(Consumer.class);
        Consumer<byte[]> consumer2 = mock(Consumer.class);
        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(consumer1).accept(isA(byte[].class));
        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(consumer2).accept(isA(byte[].class));

        topicManager.subscribe(dataDestination.getDestinationName(), dataDestination.getAccessToken(), consumer1);
        topicManager.subscribe(dataDestination.getDestinationName(), dataDestination.getAccessToken(), consumer2);

        topicManager.connect("destination", dataDestination.getAccessToken()).getContent().accept("test".getBytes());
        var result = cdl.await(10, TimeUnit.SECONDS);

        assertTrue(result);
        verify(consumer1).accept(isA(byte[].class));
        verify(consumer2).accept(isA(byte[].class));
    }

    @BeforeEach
    void setUp() {
        topicManager = new DemoTopicManager(new Monitor() {
        });
    }

    @AfterEach
    void tearDown() {
        topicManager.stop();
    }
}

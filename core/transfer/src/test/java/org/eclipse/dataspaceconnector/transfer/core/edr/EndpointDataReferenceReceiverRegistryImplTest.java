/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.edr;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiver;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transfer.core.edr.EndpointDataReferenceFixtures.createEndpointDataReference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EndpointDataReferenceReceiverRegistryImplTest {

    private static final Faker FAKER = new Faker();

    private EndpointDataReferenceReceiver receiver1;
    private EndpointDataReferenceReceiver receiver2;
    private EndpointDataReferenceReceiverRegistry registry;

    @BeforeEach
    public void setUp() {
        receiver1 = mock(EndpointDataReferenceReceiver.class);
        receiver2 = mock(EndpointDataReferenceReceiver.class);
        registry = new EndpointDataReferenceReceiverRegistryImpl();
        registry.registerReceiver(receiver1);
        registry.registerReceiver(receiver2);
    }

    @Test
    void receiveAll_success() {
        var edr = createEndpointDataReference();

        when(receiver1.send(any())).thenReturn(CompletableFuture.completedFuture(Result.success()));
        when(receiver2.send(any())).thenReturn(CompletableFuture.completedFuture(Result.success()));

        var future = registry.receiveAll(edr);

        verify(receiver1, times(1)).send(edr);
        verify(receiver2, times(1)).send(edr);

        assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).satisfies(res -> assertThat(res.succeeded()).isTrue());
    }

    @Test
    void receiveAll_failsBecauseReceiverReturnsFailedResult_shouldReturnFailedResult() {
        var edr = createEndpointDataReference();
        var errorMsg = FAKER.lorem().sentence();

        when(receiver1.send(any())).thenReturn(CompletableFuture.completedFuture(Result.success()));
        when(receiver2.send(any())).thenReturn(CompletableFuture.completedFuture(Result.failure(errorMsg)));

        var future = registry.receiveAll(edr);

        verify(receiver1, times(1)).send(edr);
        verify(receiver2, times(1)).send(edr);

        assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).satisfies(res -> {
            assertThat(res.failed()).isTrue();
            assertThat(res.getFailureMessages()).containsExactly(errorMsg);
        });
    }

    @Test
    void receiveAll_failsBecauseReceiverThrows_shouldReturnFailedResult() {
        var edr = createEndpointDataReference();
        var errorMsg = FAKER.lorem().sentence();

        when(receiver1.send(any())).thenReturn(CompletableFuture.completedFuture(Result.success()));
        when(receiver2.send(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException(errorMsg)));

        var future = registry.receiveAll(edr);

        verify(receiver1, times(1)).send(edr);
        verify(receiver2, times(1)).send(edr);

        assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).satisfies(res -> {
            assertThat(res.failed()).isTrue();
            assertThat(res.getFailureMessages().stream().anyMatch(s -> s.contains(errorMsg))).isTrue();
        });
    }
}
/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.statemachine.retry;

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimpleRetryProcessTest {

    @Test
    void shouldProcess() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        Supplier<Boolean> process = mock(Supplier.class);
        when(process.get()).thenReturn(true);
        var configuration = new EntityRetryProcessConfiguration(2, () -> () -> 2L);
        var retryProcess = new SimpleRetryProcess<>(entity, process, mock(Monitor.class), mock(Clock.class), configuration);

        var result = retryProcess.execute("any");

        assertThat(result).isTrue();
        verify(process).get();
    }
}

/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.dataplane.framework.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore.State.COMPLETED;
import static org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore.State.NOT_TRACKED;
import static org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore.State.RECEIVED;

class InMemoryDataPlaneStoreTest {
    private InMemoryDataPlaneStore store;

    @Test
    void verifyOperations() {
        assertThat(store.getState("1")).isEqualTo(NOT_TRACKED);
        store.received("1");
        assertThat(store.getState("1")).isEqualTo(RECEIVED);
        store.completed("1");
        assertThat(store.getState("1")).isEqualTo(COMPLETED);
    }

    @Test
    void verifyResetState() {
        store.completed("1");
        store.received("1");
        assertThat(store.getState("1")).isEqualTo(RECEIVED);
    }

    @Test
    void verifyNonReceivedProcess() {
        store.completed("1");
        assertThat(store.getState("1")).isEqualTo(COMPLETED);
    }

    @BeforeEach
    void setUp() {
        store = new InMemoryDataPlaneStore(2);
    }
}

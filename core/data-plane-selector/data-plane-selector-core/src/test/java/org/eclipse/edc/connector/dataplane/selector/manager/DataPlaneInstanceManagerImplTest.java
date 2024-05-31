/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.manager;

import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class DataPlaneInstanceManagerImplTest {

    private final DataPlaneInstanceStore store = mock();
    private final DataPlaneInstanceManagerImpl manager = DataPlaneInstanceManagerImpl.Builder.newInstance()
            .store(store)
            .build();

    @Nested
    class Registered {
        @Test
        void shouldTransitionToAvailable_whenDataPlaneIsAvailable() {
            //            when(store.nextNotLeased(anyInt(), stateIs()))
        }

    }
}

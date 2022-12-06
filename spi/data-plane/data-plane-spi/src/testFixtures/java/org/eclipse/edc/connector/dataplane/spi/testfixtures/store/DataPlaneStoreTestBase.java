/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.spi.testfixtures.store;

import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore.State.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore.State.NOT_TRACKED;
import static org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore.State.RECEIVED;

public abstract class DataPlaneStoreTestBase {


    @Test
    void verifyOperations() {
        assertThat(getStore().getState("1")).isEqualTo(NOT_TRACKED);
        getStore().received("1");
        assertThat(getStore().getState("1")).isEqualTo(RECEIVED);
        getStore().completed("1");
        assertThat(getStore().getState("1")).isEqualTo(COMPLETED);
    }

    @Test
    void verifyResetState() {
        getStore().completed("1");
        getStore().received("1");
        assertThat(getStore().getState("1")).isEqualTo(RECEIVED);
    }

    @Test
    void verifyNonReceivedProcess() {
        getStore().completed("1");
        assertThat(getStore().getState("1")).isEqualTo(COMPLETED);
    }

    protected abstract DataPlaneStore getStore();
}

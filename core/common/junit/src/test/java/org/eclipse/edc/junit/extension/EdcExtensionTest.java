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

package org.eclipse.edc.junit.extension;

import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.MonitorExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(EdcExtension.class)
class EdcExtensionTest {

    private EdcHttpClient testClient;

    @BeforeEach
    void setUp(EdcExtension extension) {
        testClient = mock(EdcHttpClient.class);
        // registers a mock(Monitor.class)
        var mockMonitorExtension = new MockMonitorExtension();
        extension.registerSystemExtension(MonitorExtension.class, mockMonitorExtension);
        extension.registerServiceMock(EdcHttpClient.class, testClient);
    }

    @Test
    void registerServiceMock_serviceAlreadyExists(EdcExtension extension) {
        var mockedMonitor = extension.getContext().getMonitor();
        assertThat(extension.getContext().getService(EdcHttpClient.class)).isEqualTo(testClient);
        verify(mockedMonitor, atLeastOnce()).warning(startsWith("TestServiceExtensionContext: A service mock was registered for type org.eclipse.edc.spi.http.EdcHttpClient"));
    }

    @Test
    void registerServiceMock_serviceContextReadOnlyMode(EdcExtension extension) {
        assertThatThrownBy(() -> extension.getContext().registerService(Vault.class, new InMemoryVault(mock(Monitor.class))))
                .isInstanceOf(EdcException.class)
                .hasMessageStartingWith("Cannot register service");
    }
}

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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.boot.system.runtime;

import org.eclipse.dataspaceconnector.boot.system.ServiceLocator;
import org.eclipse.dataspaceconnector.spi.system.NullVaultExtension;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseRuntimeTest {

    private final ServiceLocator serviceLocator = mock(ServiceLocator.class);
    private final BaseRuntime runtime = new BaseRuntime(serviceLocator);

    @BeforeEach
    void setUp() {
        runtime.createServiceExtensionContext();
    }

    @Test
    void loadVaultExtension_loadsNullWhenNoVaultExtensionRegistered() {
        var vaultExtension = runtime.loadVaultExtension();

        assertThat(vaultExtension).isInstanceOf(NullVaultExtension.class);
    }

    @Test
    void loadVaultExtension_loadsRegisteredVaultExtension() {
        var mockVaultExtension = mock(VaultExtension.class);
        when(serviceLocator.loadSingletonImplementor(VaultExtension.class, false)).thenReturn(mockVaultExtension);

        var vaultExtension = runtime.loadVaultExtension();

        assertThat(vaultExtension).isSameAs(mockVaultExtension);
    }
}
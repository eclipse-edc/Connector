/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.core;

import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

// needed for one particular test, to be able to verify on the core services.
public class CoreServicesDependencyInjectionExtension extends DependencyInjectionExtension {
    @Override
    protected @NotNull TypeManager createTypeManager() {
        return spy(new TypeManager());
    }

    @Override
    protected VaultExtension loadVaultExtension() {
        return new VaultExtension() {

            private final PrivateKeyResolver privateKeyResolver = mock(PrivateKeyResolver.class);

            @Override
            public Vault getVault() {
                return null;
            }

            @Override
            public PrivateKeyResolver getPrivateKeyResolver() {
                return privateKeyResolver;
            }

            @Override
            public CertificateResolver getCertificateResolver() {
                return null;
            }
        };
    }
}

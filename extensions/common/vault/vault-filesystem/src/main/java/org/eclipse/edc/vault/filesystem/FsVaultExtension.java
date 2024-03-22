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

package org.eclipse.edc.vault.filesystem;

import org.eclipse.edc.keys.spi.CertificateResolver;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Bootstraps the file system-based vault extension.
 */
@BaseExtension
@Provides({ PrivateKeyResolver.class, CertificateResolver.class })
@Extension(value = FsVaultExtension.NAME)
public class FsVaultExtension implements ServiceExtension {

    @Setting
    static final String VAULT_LOCATION = "edc.vault";

    @Setting
    static final String PERSISTENT_VAULT = "edc.vault.persistent";

    public static final String NAME = "FS Vault";


    @Override
    public String name() {
        return NAME;
    }


    @Provider
    public Vault vault(ServiceExtensionContext context) {
        var vaultLocation = context.getSetting(VAULT_LOCATION, "dataspaceconnector-vault.properties");
        var vaultPath = Paths.get(vaultLocation);
        if (!Files.exists(vaultPath)) {
            throw new EdcException("Vault file does not exist: " + vaultLocation);
        }
        var persistentVault = context.getSetting(PERSISTENT_VAULT, true);
        return new FsVault(vaultPath, persistentVault);
    }


}

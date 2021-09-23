/*
 *  Copyright (c) 2021 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Siemens AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.vault;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;

public class SingletonVaultExtension implements VaultExtension {

    private Vault vault;

    @Override
    public void initialize(Monitor monitor) {
        vault = initializeVault();

        monitor.info("Initialized Singleton Vault extension");
    }

    @Override
    public Vault getVault() {
        return vault;
    }

    @Override
    public PrivateKeyResolver getPrivateKeyResolver() {
        return null;
    }

    @Override
    public CertificateResolver getCertificateResolver() {
        return null;
    }

    private final String SECRET_PROPERTY = "creds";

    public Vault initializeVault() {
        final String secret = System.getProperty(SECRET_PROPERTY);
        final SingletonVault vault = new SingletonVault(secret);

        return vault;
    }
}

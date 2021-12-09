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

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;

public class SingletonVaultExtension implements VaultExtension {

    @EdcSetting
    private static final String CREDS = "edc.transfer.demo.s3.destination.creds";

    private Vault vault;

    @Override
    public String name() {
        return "Singleton Vault";
    }

    @Override
    public void intializeVault(ServiceExtensionContext context) {
        String secret = context.getSetting(CREDS, "default-secret");

        vault = new SingletonVault(secret);
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
}

/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;

import static org.eclipse.dataspaceconnector.core.security.hashicorpvault.HashicorpVaultExtension.VAULT_TOKEN;
import static org.eclipse.dataspaceconnector.core.security.hashicorpvault.HashicorpVaultExtension.VAULT_URL;

@ExtendWith(EdcExtension.class)
class AbstractHashicorpIT {
    static final String VAULT_TEST_URL = "http://127.0.0.1:8200";
    static final String VAULT_TEST_TOKEN = "test-token";

    final TestExtension testExtension = new TestExtension();

    @BeforeEach
    final void beforeEach(EdcExtension extension) {
        extension.setConfiguration(
                new HashMap<>() {
                    {
                        put(VAULT_URL, VAULT_TEST_URL);
                        put(VAULT_TOKEN, VAULT_TEST_TOKEN);
                    }
                });
        extension.registerSystemExtension(ServiceExtension.class, testExtension);
    }

    static class TestExtension implements ServiceExtension {
        private Vault vault;
        private CertificateResolver certificateResolver;

        @Override
        public void initialize(ServiceExtensionContext context) {
            vault = context.getService(Vault.class);
            certificateResolver = context.getService(CertificateResolver.class);
        }

        Vault getVault() {
            return this.vault;
        }

        CertificateResolver getCertificateResolver() {
            return this.certificateResolver;
        }
    }
}

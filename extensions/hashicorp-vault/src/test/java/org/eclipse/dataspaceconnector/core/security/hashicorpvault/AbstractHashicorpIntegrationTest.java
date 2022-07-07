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

import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.eclipse.dataspaceconnector.core.security.hashicorpvault.HashicorpVaultExtension.VAULT_TOKEN;
import static org.eclipse.dataspaceconnector.core.security.hashicorpvault.HashicorpVaultExtension.VAULT_URL;

@ExtendWith(EdcExtension.class)
class AbstractHashicorpIntegrationTest {
    static final String VAULT_TEST_URL = "http://127.0.0.1:8200";
    static final String VAULT_TEST_TOKEN = "test-token";
    Vault vault;
    CertificateResolver certificateResolver;

    @BeforeEach
    final void beforeEach(EdcExtension extension) {
        extension.setConfiguration(Map.of(VAULT_URL, VAULT_TEST_URL, VAULT_TOKEN, VAULT_TEST_TOKEN));

        vault = extension.getContext().getService(Vault.class);
        certificateResolver = extension.getContext().getService(CertificateResolver.class);
    }

}

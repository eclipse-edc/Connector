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

package org.eclipse.edc.iam.oauth2.daps;

import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.iam.oauth2.daps.DapsIntegrationTest.CLIENT_CERTIFICATE_ALIAS;
import static org.eclipse.edc.iam.oauth2.daps.DapsIntegrationTest.CLIENT_PRIVATE_KEY_ALIAS;

public class VaultSeedExtension implements ServiceExtension {

    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var certificate = TestUtils.getResourceFileContentAsString("certificate.pem");
        var privateKey = TestUtils.getResourceFileContentAsString("privatekey.pem");
        vault.storeSecret(CLIENT_CERTIFICATE_ALIAS, certificate);
        vault.storeSecret(CLIENT_PRIVATE_KEY_ALIAS, privateKey);
    }
}

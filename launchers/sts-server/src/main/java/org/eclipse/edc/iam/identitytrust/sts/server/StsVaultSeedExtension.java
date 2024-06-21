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

package org.eclipse.edc.iam.identitytrust.sts.server;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Map;

public class StsVaultSeedExtension implements ServiceExtension {


    public static final String VAULT_TESTING_PREFIX = "edc.sts.server.vaults";

    public static final String VAULT_TESTING_KEY = "key";
    public static final String VAULT_TESTING_VALUE = "value";

    @Inject
    private Vault vault;


    @Override
    public void initialize(ServiceExtensionContext context) {

        var config = context.getConfig(VAULT_TESTING_PREFIX);
        var secrets = config.partition().map((partition) -> {
            var key = partition.getString(VAULT_TESTING_KEY);
            var value = partition.getString(VAULT_TESTING_VALUE);
            return Map.entry(key, value);
        }).toList();

        secrets.forEach(secret -> vault.storeSecret(secret.getKey(), secret.getValue()));
    }
}
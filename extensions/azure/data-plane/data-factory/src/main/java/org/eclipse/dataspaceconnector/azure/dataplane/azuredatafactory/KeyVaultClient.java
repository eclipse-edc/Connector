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

package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

/**
 * Client for Azure Key Vault, wrapping the Azure SDK.
 */
class KeyVaultClient {
    private final SecretClient secretClient;

    KeyVaultClient(SecretClient secretClient) {
        this.secretClient = secretClient;
    }

    /**
     * Sets a Key Vault secret.
     *
     * @param name secret name.
     * @param value secret value.
     * @return created Key Vault secret.
     */
    KeyVaultSecret setSecret(String name, String value) {
        return secretClient.setSecret(name, value);
    }
}
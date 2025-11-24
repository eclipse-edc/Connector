/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.MediaType;

import java.util.Map;

public interface VaultConstants {
    TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };
    String VAULT_TOKEN_HEADER = "X-Vault-Token";
    String VAULT_REQUEST_HEADER = "X-Vault-Request";
    String VAULT_SECRET_METADATA_PATH = "metadata";
    MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json");
    String VAULT_CONFIG = "vaultConfig";

}

/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

/**
 * Configuration for Hashicorp Vault.
 */
interface HashicorpVaultConfig {

    boolean VAULT_HEALTH_CHECK_ENABLED_DEFAULT = true;
    boolean VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT = false;
    String VAULT_API_HEALTH_PATH_DEFAULT = "/v1/sys/health";
    boolean VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED_DEFAULT = true;
    long VAULT_TOKEN_RENEW_BUFFER_DEFAULT = 30;
    long VAULT_TOKEN_TTL_DEFAULT = 300;
    String VAULT_API_SECRET_PATH_DEFAULT = "/v1/secret";

    @Setting(value = "The URL of the Hashicorp Vault", required = true)
    String VAULT_URL = "edc.vault.hashicorp.url";

    @Setting(value = "Whether or not the vault health check is enabled", defaultValue = "true", type = "boolean")
    String VAULT_HEALTH_CHECK_ENABLED = "edc.vault.hashicorp.health.check.enabled";

    @Setting(value = "The URL path of the vault's /health endpoint", defaultValue = VAULT_API_HEALTH_PATH_DEFAULT)
    String VAULT_API_HEALTH_PATH = "edc.vault.hashicorp.api.health.check.path";

    @Setting(value = "Specifies if being a standby should still return the active status code instead of the standby status code", defaultValue = "false", type = "boolean")
    String VAULT_HEALTH_CHECK_STANDBY_OK = "edc.vault.hashicorp.health.check.standby.ok";

    @Setting(value = "The token used to access the Hashicorp Vault", required = true)
    String VAULT_TOKEN = "edc.vault.hashicorp.token";

    @Setting(value = "Whether the automatic token renewal process will be triggered or not. Should be disabled only for development and testing purposes", defaultValue = "true")
    String VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED = "edc.vault.hashicorp.token.scheduled-renew-enabled";

    @Setting(value = "The time-to-live (ttl) value of the Hashicorp Vault token in seconds", defaultValue = "300", type = "long")
    String VAULT_TOKEN_TTL = "edc.vault.hashicorp.token.ttl";

    @Setting(value = "The renew buffer of the Hashicorp Vault token in seconds", defaultValue = "30", type = "long")
    String VAULT_TOKEN_RENEW_BUFFER = "edc.vault.hashicorp.token.renew-buffer";

    @Setting(value = "The URL path of the vault's /secret endpoint", defaultValue = VAULT_API_SECRET_PATH_DEFAULT)
    String VAULT_API_SECRET_PATH = "edc.vault.hashicorp.api.secret.path";
}

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
 *
 */

package org.eclipse.edc.vault.hashicorp.model;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

public interface Constants {
    String VAULT_API_SECRET_PATH_DEFAULT = "/v1/secret";
    String VAULT_API_HEALTH_PATH_DEFAULT = "/v1/sys/health";
    boolean VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT = false;
    int VAULT_TIMEOUT_SECONDS_DEFAULT = 30;
    @Setting(value = "The URL path of the vault's /secret endpoint", defaultValue = VAULT_API_SECRET_PATH_DEFAULT)
    String VAULT_API_SECRET_PATH = "edc.vault.hashicorp.api.secret.path";

    @Setting(value = "The URL path of the vault's /health endpoint", defaultValue = VAULT_API_HEALTH_PATH_DEFAULT)
    String VAULT_API_HEALTH_PATH = "edc.vault.hashicorp.api.health.check.path";

    @Setting(value = "Specifies if being a standby should still return the active status code instead of the standby status code", defaultValue = "false", type = "boolean")
    String VAULT_HEALTH_CHECK_STANDBY_OK = "edc.vault.hashicorp.health.check.standby.ok";

    @Setting(value = "Sets the timeout for HTTP requests to the vault, in seconds", defaultValue = "30", type = "integer")
    String VAULT_TIMEOUT_SECONDS = "edc.vault.hashicorp.timeout.seconds";

    @Setting(value = "The URL of the Hashicorp Vault", required = true)
    String VAULT_URL = "edc.vault.hashicorp.url";

    @Setting(value = "The token used to access the Hashicorp Vault", required = true)
    String VAULT_TOKEN = "edc.vault.hashicorp.token";
}

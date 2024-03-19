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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Provides({ CertificateResolver.class })
@Extension(value = HashicorpVaultExtension.NAME)
public class HashicorpVaultExtension implements ServiceExtension {

    public static final String NAME = "Hashicorp Vault";
    public static final String VAULT_API_SECRET_PATH_DEFAULT = "/v1/secret";
    public static final int VAULT_TIMEOUT_SECONDS_DEFAULT = 30;
    @Setting(value = "The URL path of the vault's /secret endpoint", defaultValue = VAULT_API_SECRET_PATH_DEFAULT)
    public static final String VAULT_API_SECRET_PATH = "edc.vault.hashicorp.api.secret.path";
    @Setting(value = "Sets the timeout for HTTP requests to the vault, in seconds", defaultValue = "30", type = "integer")
    public static final String VAULT_TIMEOUT_SECONDS = "edc.vault.hashicorp.timeout.seconds";
    @Setting(value = "The URL of the Hashicorp Vault", required = true)
    public static final String VAULT_URL = "edc.vault.hashicorp.url";
    @Setting(value = "The token used to access the Hashicorp Vault", required = true)
    public static final String VAULT_TOKEN = "edc.vault.hashicorp.token";


    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public Vault vault() {
        return vault;
    }

    @Provider
    public Vault hashicorpVault(ServiceExtensionContext context) {
        if (vault == null) {
            var config = HashicorpVaultClientConfig.create(context);
            var client = new HashicorpVaultClient(config, httpClient, typeManager.getMapper());

            vault = new HashicorpVault(client, context.getMonitor());
        }
        return vault;
    }

    @Provider
    public CertificateResolver vaultResolver(ServiceExtensionContext context) {
        return new HashicorpCertificateResolver(hashicorpVault(context), context.getMonitor().withPrefix("HashicorpVaultCertificateResolver"));
    }

}

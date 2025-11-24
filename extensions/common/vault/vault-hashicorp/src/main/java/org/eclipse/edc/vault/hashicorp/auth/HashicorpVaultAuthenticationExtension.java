/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.auth;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;

import static java.util.Objects.requireNonNull;

@Extension(value = HashicorpVaultAuthenticationExtension.NAME)
public class HashicorpVaultAuthenticationExtension implements ServiceExtension {

    public static final String NAME = "Hashicorp Vault Authentication";

    @Setting(description = "The token used to access the Hashicorp Vault. Only required, if default token authentication is used.", key = "edc.vault.hashicorp.token", required = false)
    private String token;

    @Setting(description = "Client-ID to use when obtaining an OAuth2 JWT for Vault access", key = "edc.vault.hashicorp.clientid", required = false)
    private String clientId;

    @Setting(description = "Client-Secret to use when obtaining an OAuth2 JWT for Vault access", key = "edc.vault.hashicorp.clientsecret", required = false)
    private String clientSecret;

    @Setting(description = "URL of the OAuth2 token endpoint", key = "edc.vault.hashicorp.tokenurl", required = false)
    private String tokenUrl;

    @Inject(required = false)
    private EdcHttpClient edcHttpClient;

    @Provider(isDefault = true)
    public HashicorpVaultTokenProvider tokenProvider() {

        var hasOauth = clientId != null && clientSecret != null && tokenUrl != null;
        if (!hasOauth && token == null) {
            throw new IllegalArgumentException("Either edc.vault.hashicorp.token or (.clientId, .clientSecret and .tokenUrl) must be provided");
        }

        if (hasOauth & token != null) {
            throw new IllegalArgumentException("Only one of edc.vault.hashicorp.token or (.clientId, .clientSecret and .tokenUrl) must be provided");
        }

        if (token != null) {
            return new HashicorpVaultTokenProviderImpl(token);
        }

        requireNonNull(edcHttpClient, "EdcHttpClient must be provided to use OAuth2 authentication");
        return OauthTokenProvider.Builder.newInstance().clientId(clientId).clientSecret(clientSecret).tokenUrl(tokenUrl).httpClient(edcHttpClient).build();
    }
}

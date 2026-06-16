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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProviderFactory;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.Objects.requireNonNull;

@Extension(value = HashicorpVaultAuthenticationExtension.NAME)
public class HashicorpVaultAuthenticationExtension implements ServiceExtension {

    public static final String NAME = "Hashicorp Vault Authentication";

    @Setting(description = "The token used to access the Hashicorp Vault. Only required if static token authentication is used.", key = "edc.vault.hashicorp.token", required = false)
    private String token;

    @Setting(description = "The URL of the Hashicorp Vault. Required if token-exchange authentication is used.", key = "edc.vault.hashicorp.url", required = false)
    private String vaultUrl;

    @Setting(description = "URL of the token-exchange service (e.g. jwtlet) used to exchange the workload token for a participant-scoped token", key = "edc.vault.hashicorp.auth.tokenexchange.url", required = false)
    private String tokenExchangeUrl;

    @Setting(description = "Path to the file holding the projected workload (ServiceAccount) token used as the subject token in the exchange", key = "edc.vault.hashicorp.auth.tokenexchange.subjecttokenpath", required = false)
    private String subjectTokenPath;

    @Setting(description = "The audience requested for the exchanged token. Must match the token-exchange service's configured audience.",
            key = "edc.vault.hashicorp.auth.tokenexchange.audience", defaultValue = HashicorpVaultAuthenticationExtension.DEFAULT_AUDIENCE)
    private String audience;

    @Setting(description = "The abstract scope (tier) requested for the exchanged token", key = "edc.vault.hashicorp.auth.tokenexchange.scope", defaultValue = HashicorpVaultAuthenticationExtension.DEFAULT_SCOPE)
    private String scope;

    @Setting(description = "The resource (participant context id) used for the default vault partition. Only relevant when token exchange is used for the default partition.",
            key = "edc.vault.hashicorp.auth.tokenexchange.resource", required = false)
    private String resource;

    @Setting(description = "The Hashicorp Vault JWT auth method role to authenticate with", key = "edc.vault.hashicorp.auth.jwt.role", defaultValue = HashicorpVaultAuthenticationExtension.DEFAULT_ROLE)
    private String role;

    @Inject(required = false)
    private EdcHttpClient edcHttpClient;

    static final String DEFAULT_AUDIENCE = "edcv";
    static final String DEFAULT_SCOPE = "read";
    static final String DEFAULT_ROLE = "participant";

    @Provider(isDefault = true)
    public HashicorpVaultTokenProviderFactory tokenProviderFactory() {
        var hasTokenExchange = tokenExchangeUrl != null;
        if (!hasTokenExchange && token == null) {
            throw new IllegalArgumentException("Either edc.vault.hashicorp.token or edc.vault.hashicorp.auth.tokenexchange.url must be provided");
        }

        var builder = HashicorpVaultTokenProviderFactoryImpl.Builder.newInstance()
                .staticToken(token);

        if (hasTokenExchange) {
            requireNonNull(edcHttpClient, "EdcHttpClient must be provided to use token-exchange authentication");
            requireNonNull(vaultUrl, "edc.vault.hashicorp.url must be provided to use token-exchange authentication");
            requireNonNull(subjectTokenPath, "edc.vault.hashicorp.auth.tokenexchange.subjecttokenpath must be provided to use token-exchange authentication");
            builder.tokenExchangeUrl(tokenExchangeUrl)
                    .subjectTokenPath(subjectTokenPath)
                    .audience(audience)
                    .scope(scope)
                    .role(role)
                    .defaultResource(resource)
                    .vaultUrl(vaultUrl)
                    .httpClient(edcHttpClient)
                    .objectMapper(new ObjectMapper().disable(FAIL_ON_UNKNOWN_PROPERTIES));
        }

        return builder.build();
    }
}

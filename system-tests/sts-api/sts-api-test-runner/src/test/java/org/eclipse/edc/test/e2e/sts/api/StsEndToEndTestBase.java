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

package org.eclipse.edc.test.e2e.sts.api;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.security.Vault;

import java.text.ParseException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClient;

/**
 * Base class for STS E2E tests
 */
public abstract class StsEndToEndTestBase {

    protected abstract RuntimePerClassExtension getRuntime();

    protected StsClient initClient(String clientId, String clientSecret) {
        var store = getClientStore();
        var vault = getVault();
        var clientSecretAlias = "client_secret_alias";
        var client = createClient(clientId, clientSecretAlias);

        vault.storeSecret(clientSecretAlias, clientSecret);
        vault.storeSecret(client.getPrivateKeyAlias(), loadResourceFile("ec-privatekey.pem"));
        store.create(client);

        return client;
    }

    protected StsClient initClient(String clientSecret) {
        return initClient(UUID.randomUUID().toString(), clientSecret);
    }

    protected Map<String, Object> parseClaims(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet().getClaims();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private StsClientStore getClientStore() {
        return getRuntime().getService(StsClientStore.class);
    }

    private Vault getVault() {
        return getRuntime().getService(Vault.class);
    }

    /**
     * Load content from a resource file.
     */
    private String loadResourceFile(String file) {
        try (var resourceAsStream = RemoteStsEndToEndTest.class.getClassLoader().getResourceAsStream(file)) {
            return new String(Objects.requireNonNull(resourceAsStream).readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

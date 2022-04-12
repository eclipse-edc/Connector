/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.core;

import com.github.javafaker.Faker;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.JwtDecorator;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.junit.launcher.MockVault;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferTokenGenerator;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferTokenValidator;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EdcExtension.class)
class DataPlaneTransferCoreExtensionTest {

    private static final String PUBLIC_KEY_FILE = "publickey.pem";
    private static final String PRIVATE_KEY_FILE = "privatekey.pem";

    private static final Faker FAKER = new Faker();

    @BeforeEach
    public void setUp(EdcExtension extension) {
        String publicKeyAlias = FAKER.internet().uuid();
        String privateKeyAlias = FAKER.internet().uuid();

        System.setProperty("edc.transfer.proxy.endpoint", FAKER.internet().url());
        System.setProperty("edc.transfer.proxy.token.signer.privatekey.alias", privateKeyAlias);
        System.setProperty("edc.transfer.proxy.token.verifier.publickey.alias", publicKeyAlias);

        var publicKey = loadResourceFile(PUBLIC_KEY_FILE);
        var privateKey = loadResourceFile(PRIVATE_KEY_FILE);
        var vault = new MockVault();
        vault.storeSecret(publicKeyAlias, publicKey);
        vault.storeSecret(privateKeyAlias, privateKey);
        extension.registerServiceMock(PrivateKeyResolver.class, new PrivateKeyResolver() {
            @Override
            public <T> @Nullable T resolvePrivateKey(String id, Class<T> keyType) {
                return null;
            }
        });
        extension.registerServiceMock(Vault.class, vault);
    }


    @AfterAll
    static void unsetProps() {
        System.clearProperty("edc.transfer.proxy.endpoint");
        System.clearProperty("edc.transfer.proxy.token.signer.privatekey.alias");
        System.clearProperty("edc.transfer.proxy.token.verifier.publickey.alias");
    }

    /**
     * Ensure proper signed token generation and validation with associated public key.
     */
    @Test
    void tokenGenerationAndValidation_success(DataPlaneTransferTokenGenerator tokenGenerator, DataPlaneTransferTokenValidator tokenValidator) {
        var tokenGenerationResult = tokenGenerator.generate(new FakeDecorator());
        assertThat(tokenGenerationResult.succeeded()).isTrue();

        var token = tokenGenerationResult.getContent();

        var tokenValidationResult = tokenValidator.validate(token.getToken());
        assertThat(tokenValidationResult.succeeded()).isTrue();
    }

    /**
     * Sample JWT decorator
     */
    private static class FakeDecorator implements JwtDecorator {
        @Override
        public void decorate(JWSHeader.Builder header, JWTClaimsSet.Builder claimsSet) {
            claimsSet.claim("foo", "bar");
        }
    }

    private static String loadResourceFile(String file) {
        try (InputStream in = DataPlaneTransferCoreExtensionTest.class.getClassLoader().getResourceAsStream(file)) {
            Objects.requireNonNull(in);
            return new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
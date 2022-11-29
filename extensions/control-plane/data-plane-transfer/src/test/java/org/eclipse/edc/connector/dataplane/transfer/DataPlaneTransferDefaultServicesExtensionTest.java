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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.transfer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.transfer.DataPlaneTransferConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.connector.dataplane.transfer.DataPlaneTransferConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneTransferDefaultServicesExtensionTest {

    private ServiceExtensionContext context;
    private PrivateKeyResolver privateKeyResolver;
    private Vault vault;
    private DataPlaneTransferDefaultServicesExtension extension;

    @BeforeEach
    public void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        var monitor = mock(Monitor.class);
        this.privateKeyResolver = mock(PrivateKeyResolver.class);
        this.vault = mock(Vault.class);

        context.registerService(PrivateKeyResolver.class, privateKeyResolver);
        context.registerService(Vault.class, vault);

        this.context = spy(context); //used to inject the config
        when(this.context.getMonitor()).thenReturn(monitor);

        extension = factory.constructInstance(DataPlaneTransferDefaultServicesExtension.class);
    }

    @Test
    void verifyRandomKeyPairWrapperIfNoSettingProvided() {
        var wrapper = extension.getKeyPairWrapper(context);

        var keys = wrapper.get();
        assertThat(keys).isNotNull();
        assertThat(keys.getPrivate())
                .isNotNull()
                .isInstanceOf(ECPrivateKey.class);
        assertThat(keys.getPublic())
                .isNotNull()
                .isInstanceOf(ECPublicKey.class);
    }

    @Test
    void verifyExtractKeysFromConfig() throws JOSEException, IOException {
        var keyPair = generateRandomKeyPair();
        var privateKeyAlias = "priv-key-test";
        var publicKeyAlias = "pub-key-test";

        var publicKeyPem = toPemEncoded(keyPair.getPublic());
        when(vault.resolveSecret(publicKeyAlias)).thenReturn(publicKeyPem);
        when(privateKeyResolver.resolvePrivateKey(privateKeyAlias, PrivateKey.class)).thenReturn(keyPair.getPrivate());

        var config = ConfigFactory.fromMap(Map.of(
                TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, publicKeyAlias,
                TOKEN_SIGNER_PRIVATE_KEY_ALIAS, privateKeyAlias)
        );
        when(context.getConfig()).thenReturn(config);

        var wrapper = extension.getKeyPairWrapper(context);

        var keys = wrapper.get();
        assertThat(keys).isNotNull();
        assertThat(keys.getPrivate())
                .isNotNull()
                .isInstanceOf(RSAPrivateKey.class);
        assertThat(keys.getPublic())
                .isNotNull()
                .isInstanceOf(RSAPublicKey.class);
    }

    private String toPemEncoded(Key key) throws IOException {
        var writer = new StringWriter();
        try (var jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(key);
        }

        return writer.toString();
    }

    private static KeyPair generateRandomKeyPair() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate()
                .toKeyPair();
    }
}
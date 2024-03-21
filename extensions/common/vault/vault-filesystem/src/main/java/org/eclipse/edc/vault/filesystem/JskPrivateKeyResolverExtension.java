/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.vault.filesystem;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static org.eclipse.edc.vault.filesystem.JskPrivateKeyResolverExtension.NAME;

@Extension(NAME)
public class JskPrivateKeyResolverExtension implements ServiceExtension {
    public static final String NAME = "JKS PrivateKeyResolver Extension";

    @Setting
    static final String KEYSTORE_LOCATION = "edc.keystore";

    @Setting
    static final String KEYSTORE_PASSWORD = "edc.keystore.password";

    @Inject
    private KeyParserRegistry registry;
    private KeyStore keyStore;


    @Override
    public void initialize(ServiceExtensionContext context) {

        var monitor = context.getMonitor();
        monitor.warning("Using the JSK-based Vault and PrivateKeyResolver is intended only for testing and demo purposes. Do NOT use this in a production scenario!");

        keyStore = loadKeyStore(context);

    }

    @Provider
    public PrivateKeyResolver createResolver(ServiceExtensionContext context) {
        var keystorePassword = context.getSetting(KEYSTORE_PASSWORD, null);
        return new JksPrivateKeyResolver(registry, keystorePassword, keyStore, context.getConfig(), context.getMonitor().withPrefix("PrivateKeyResolution"));
    }

    @Provider
    public CertificateResolver createCertificateResolver() {
        return new FsCertificateResolver(keyStore);
    }

    private KeyStore loadKeyStore(ServiceExtensionContext context) {
        var keyStoreLocation = context.getSetting(KEYSTORE_LOCATION, "dataspaceconnector-keystore.jks");
        var keyStorePath = Paths.get(keyStoreLocation);
        if (!Files.exists(keyStorePath)) {
            throw new EdcException("Key store does not exist: " + keyStoreLocation);
        }

        var keystorePassword = context.getSetting(KEYSTORE_PASSWORD, null);
        if (keystorePassword == null) {
            throw new EdcException("Key store password was not specified");
        }

        try (var stream = Files.newInputStream(keyStorePath)) {
            var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(stream, keystorePassword.toCharArray());
            return keyStore;
        } catch (IOException | GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }
}

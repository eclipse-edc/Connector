/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *
 */

package org.eclipse.edc.vault.filesystem;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.edc.keys.AbstractPrivateKeyResolver;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * Resolves an RSA or EC private key from a JKS keystore. This is not suitable for production environments, because the keystore
 * password is kept in memory for subsequent queries against the JKS. In addition, the {@link KeyStore} will not work in a clustered environment.
 */
public class JksPrivateKeyResolver extends AbstractPrivateKeyResolver {

    private final String password;
    private final KeyStore keyStore;
    private final Monitor monitor;

    /**
     * Constructor.
     * Caches the private keys for performance.
     *
     * @param password the keystore password. Individual key passwords are not supported.
     * @param keyStore the keystore
     * @param config   The config, for resolving the private key in case of fallback
     * @param monitor  the monitor
     */
    public JksPrivateKeyResolver(KeyParserRegistry registry, String password, KeyStore keyStore, Config config, Monitor monitor) {
        super(registry, config, monitor);
        this.password = password;
        this.keyStore = keyStore;
        this.monitor = monitor;
    }

    @NotNull
    @Override
    protected Result<String> resolveInternal(String keyId) {
        var encodedPwd = password.toCharArray();

        try {
            var iter = keyStore.aliases();
            while (iter.hasMoreElements()) {
                var alias = iter.nextElement();
                if (!keyStore.isKeyEntry(alias) || !alias.equals(keyId)) {
                    continue;
                }
                // convert to PEM string so that the base class can interpret it.
                var key = keyStore.getKey(alias, encodedPwd);
                var out = new StringWriter();
                var pw = new JcaPEMWriter(out);
                pw.writeObject(key);
                pw.close();
                return Result.success(out.toString());
            }
            return Result.failure("Private Key with ID '%s' not found in KeyStore.".formatted(keyId));

        } catch (GeneralSecurityException | IOException e) {
            monitor.warning("Error resolving key from KeyStore", e);
            return Result.failure("Error resolving key from KeyStore: " + e.getMessage());
        }
    }
}

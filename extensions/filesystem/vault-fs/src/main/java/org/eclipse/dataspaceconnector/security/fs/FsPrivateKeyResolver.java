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
 *
 */

package org.eclipse.dataspaceconnector.security.fs;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves an RSA private key from a JKS keystore.
 */
public class FsPrivateKeyResolver implements PrivateKeyResolver {
    private final Map<String, RSAPrivateKey> privateKeyCache = new HashMap<>();

    /**
     * Constructor.
     * <p>
     * Caches the private keys for performance.
     *
     * @param password the keystore password. Individual key passwords are not supported.
     * @param keyStore the keystore
     */
    public FsPrivateKeyResolver(String password, KeyStore keyStore) {
        char[] encodedPassword = password.toCharArray();
        try {
            Enumeration<String> iter = keyStore.aliases();
            while (iter.hasMoreElements()) {
                String alias = iter.nextElement();
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }
                Key key = keyStore.getKey(alias, encodedPassword);
                if ((key instanceof RSAPrivateKey)) {
                    privateKeyCache.put(alias, (RSAPrivateKey) key);
                }
            }

        } catch (GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public RSAPrivateKey resolvePrivateKey(String id) {
        return privateKeyCache.get(id);
    }
}

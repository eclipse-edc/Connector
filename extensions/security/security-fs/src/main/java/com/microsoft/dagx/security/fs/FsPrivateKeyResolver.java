/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.security.fs;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;

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
    private Map<String, RSAPrivateKey> privateKeyCache = new HashMap<>();

    /**
     * Constructor.
     *
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
            throw new DagxException(e);
        }
    }

    @Override
    public RSAPrivateKey resolvePrivateKey(String id) {
        return privateKeyCache.get(id);
    }
}

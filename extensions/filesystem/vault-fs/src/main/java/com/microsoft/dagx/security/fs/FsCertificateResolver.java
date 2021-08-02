/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.security.fs;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.security.CertificateResolver;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves an X509 certificate from a JKS keystore.
 */
public class FsCertificateResolver implements CertificateResolver {
    protected Map<String, X509Certificate> certCache = new HashMap<>();

    /**
     * Constructor.
     *
     * @param keyStore the keystore
     */
    public FsCertificateResolver(KeyStore keyStore) {
        try {
            Enumeration<String> iter = keyStore.aliases();
            while (iter.hasMoreElements()) {
                String alias = iter.nextElement();
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }
                Certificate certificate = keyStore.getCertificate(alias);
                if (certificate instanceof X509Certificate) {
                    certCache.put(alias, (X509Certificate) certificate);
                }
            }
        } catch (GeneralSecurityException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public X509Certificate resolveCertificate(String id) {
        return certCache.get(id);
    }
}

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

package org.eclipse.edc.vault.filesystem;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.CertificateResolver;

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
            throw new EdcException(e);
        }
    }

    @Override
    public X509Certificate resolveCertificate(String id) {
        return certCache.get(id);
    }
}

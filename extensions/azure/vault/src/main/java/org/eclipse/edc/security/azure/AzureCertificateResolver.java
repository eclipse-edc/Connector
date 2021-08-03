/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.security.azure;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.Vault;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Resolves an X.509 certificate in Azure vault.
 */
public class AzureCertificateResolver implements CertificateResolver {
    private static final String HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String FOOTER = "-----END CERTIFICATE-----";
    private Vault vault;

    public AzureCertificateResolver(Vault vault) {
        this.vault = vault;
    }

    @Override
    public X509Certificate resolveCertificate(String id) {
        try {
            String encoded = vault.resolveSecret(id);
            if (encoded == null) {
                return null;
            }
            encoded = encoded.replace(HEADER, "").replaceAll(System.lineSeparator(), "").replace(FOOTER, "");

            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            return (X509Certificate) fact.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(encoded.getBytes())));
        } catch (GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }
}

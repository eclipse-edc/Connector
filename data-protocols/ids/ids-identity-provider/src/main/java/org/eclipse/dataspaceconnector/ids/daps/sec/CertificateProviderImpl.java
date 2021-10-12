package org.eclipse.dataspaceconnector.ids.daps.sec;

import java.security.cert.X509Certificate;

public class CertificateProviderImpl implements CertificateProvider {

    private final X509Certificate x509Certificate;

    public CertificateProviderImpl(final X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    @Override
    public X509Certificate getCertificate() {
        return x509Certificate;
    }
}

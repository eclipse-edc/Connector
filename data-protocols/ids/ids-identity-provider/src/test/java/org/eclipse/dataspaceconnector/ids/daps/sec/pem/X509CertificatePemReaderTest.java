package org.eclipse.dataspaceconnector.ids.daps.sec.pem;

import org.eclipse.dataspaceconnector.ids.daps.sec.pem.X509CertificatePemReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class X509CertificatePemReaderTest extends AbstractResourceLoadingTest{

    @Test
    void readX509Certificate() throws IOException, CertificateException {
        final X509CertificatePemReader x509CertificatePemReader = new X509CertificatePemReader();

        final X509Certificate certificate;
        try(final InputStream inputStream = getResource("test-certificate.crt")) {
            certificate = x509CertificatePemReader.readX509Certificate(inputStream);
        }

        assertNotNull(certificate);
    }

}
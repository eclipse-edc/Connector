package org.eclipse.dataspaceconnector.ids.daps.sec.pem;

import org.bouncycastle.util.io.pem.PemObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class X509CertificatePemReader extends PemReader {
    private static final String CERTIFICATE_TYPE = "X.509";

    public X509Certificate readX509Certificate(final InputStream inputStream) throws IOException, CertificateException {
        final PemObject pemObject = read(inputStream);
        final byte[] content = pemObject.getContent();
        try (final InputStream contentInputStream = new ByteArrayInputStream(content)) {
            return (X509Certificate) CertificateFactory.getInstance(CERTIFICATE_TYPE)
                    .generateCertificate(contentInputStream);
        }
    }
}

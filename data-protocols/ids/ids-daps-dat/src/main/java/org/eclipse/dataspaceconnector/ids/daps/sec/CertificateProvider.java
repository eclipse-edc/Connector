package org.eclipse.dataspaceconnector.ids.daps.sec;

import java.security.cert.X509Certificate;

public interface CertificateProvider {

    X509Certificate getCertificate() throws Exception;
}

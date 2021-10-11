package org.eclipse.dataspaceconnector.ids.daps.client;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.eclipse.dataspaceconnector.ids.daps.sec.CertificateProvider;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class X509CertificateIssuerProvider implements IssuerProvider {
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    private static final String DELIMITER = ":";
    private static final String KEY_ID = "keyid";

    private final CertificateProvider certificateProvider;

    public X509CertificateIssuerProvider(final CertificateProvider certificateProvider) {
        Objects.requireNonNull(certificateProvider);

        this.certificateProvider = certificateProvider;
    }

    @Override
    public String getIssuer() {
        try {
            final X509Certificate x509Certificate = certificateProvider.getCertificate();

            final byte[] authorityKeyIdentifier = getCertificateAKI(x509Certificate);
            final byte[] subjectKeyIdentifier = getCertificateSKI(x509Certificate);

            return generateConnectorFingerprint(authorityKeyIdentifier, subjectKeyIdentifier);
        } catch (final Exception exception) {
            throw new DapsClientException(
                    String.format("Could not create issuer claim: %s", exception.getMessage()),
                    exception);
        }
    }

    /*@VisibleForTesting*/
    static String generateConnectorFingerprint(final byte[] authorityKeyIdentifier, final byte[] subjectKeyIdentifier) {
        final String subjectKeyIdentifierString = toDelimitedHexString(subjectKeyIdentifier);
        final String authorityKeyIdentifierString = toDelimitedHexString(authorityKeyIdentifier);

        return String.join(DELIMITER, subjectKeyIdentifierString, KEY_ID, authorityKeyIdentifierString);
    }

    /*@VisibleForTesting*/
    static byte[] getCertificateSKI(final X509Certificate x509Certificate) {
        final String subjectKeyIdentifierId = Extension.subjectKeyIdentifier.getId();
        final byte[] extensionValue = x509Certificate.getExtensionValue(subjectKeyIdentifierId);
        final ASN1OctetString asn1OctetString = ASN1OctetString.getInstance(extensionValue);
        final SubjectKeyIdentifier subjectKeyIdentifier = SubjectKeyIdentifier.getInstance(asn1OctetString.getOctets());

        return subjectKeyIdentifier.getKeyIdentifier();
    }

    /*@VisibleForTesting*/
    static byte[] getCertificateAKI(final X509Certificate x509Certificate) {
        final String authorityKeyIdentifierId = Extension.authorityKeyIdentifier.getId();
        final byte[] extensionValue = x509Certificate.getExtensionValue(authorityKeyIdentifierId);
        final ASN1OctetString asn1OctetString = ASN1OctetString.getInstance(extensionValue);
        final AuthorityKeyIdentifier authorityKeyIdentifier = AuthorityKeyIdentifier.getInstance(asn1OctetString.getOctets());

        return authorityKeyIdentifier.getKeyIdentifier();
    }

    /*@VisibleForTesting*/
    static String toDelimitedHexString(final byte[] bytes) {
        final List<String> parts = new LinkedList<>();
        for (final byte b : bytes) {
            int value = b & 0xFF;
            parts.add(new String(new byte[]{ HEX_ARRAY[value >>> 4], HEX_ARRAY[value & 0x0F] }, StandardCharsets.UTF_8));
        }
        return String.join(DELIMITER, parts);
    }
}

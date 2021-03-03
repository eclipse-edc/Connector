package com.microsoft.dagx.iam.oauth2.impl;

import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static java.lang.String.format;

/**
 * Shim from a {@link PrivateKeyResolver} and {@link CertificateResolver} to a {@link RSAKeyProvider} required by the JWT signer.
 */
public class PairedProviderWrapper implements RSAKeyProvider {
    private PrivateKeyResolver privateKeyResolver;
    private CertificateResolver certificateResolver;
    private String privateKeyId;

    public PairedProviderWrapper(PrivateKeyResolver privateKeyResolver, CertificateResolver certificateResolver, String privateKeyId) {
        this.privateKeyResolver = privateKeyResolver;
        this.certificateResolver = certificateResolver;
        this.privateKeyId = privateKeyId;
    }

    @Override
    public RSAPublicKey getPublicKeyById(String keyId) {
        X509Certificate certificate = certificateResolver.resolveCertificate(keyId);
        if (certificate == null) {
            return null;
        }
        PublicKey publicKey = certificate.getPublicKey();
        if (publicKey instanceof RSAPublicKey) {
            return (RSAPublicKey) publicKey;
        }
        throw new DagxException(format("Unsupported certificate type for id %s: %s", keyId, publicKey.getClass().getName()));
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return privateKeyResolver != null ? privateKeyResolver.resolvePrivateKey(privateKeyId) : null;
    }

    @Override
    public String getPrivateKeyId() {
        return privateKeyId;
    }
}


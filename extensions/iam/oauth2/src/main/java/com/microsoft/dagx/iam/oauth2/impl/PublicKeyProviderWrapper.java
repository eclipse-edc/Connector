package com.microsoft.dagx.iam.oauth2.impl;

import com.auth0.jwt.interfaces.RSAKeyProvider;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Shim from an {@link PublicKeyResolver} to a {@link RSAKeyProvider} required by the JWT verifier.
 */
public class PublicKeyProviderWrapper implements RSAKeyProvider {
    private PublicKeyResolver resolver;

    public PublicKeyProviderWrapper(PublicKeyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public RSAPublicKey getPublicKeyById(String id) {
        return resolver.resolveKey(id);
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return null;
    }

    @Override
    public String getPrivateKeyId() {
        return null;
    }
}

package org.eclipse.dataspaceconnector.iam.did.hub;

import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidPublicKeyResolver;

import java.security.PublicKey;

/**
 *
 */
public class DidPublicKeyResolverImpl implements DidPublicKeyResolver {
    private PublicKey publicKey;

    @Override
    public PublicKey resolvePublicKey(String did) {
        return publicKey;
    }

    public DidPublicKeyResolverImpl(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}

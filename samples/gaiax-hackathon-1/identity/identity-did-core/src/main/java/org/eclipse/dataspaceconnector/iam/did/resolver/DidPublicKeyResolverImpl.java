package org.eclipse.dataspaceconnector.iam.did.resolver;

import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidPublicKeyResolver;

import java.security.PublicKey;

/**
 * TODO HACKATHON-1 This implementation needs to resolve the key by resolving the DID and loading the public key contained in the DID document.
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

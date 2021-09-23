package org.eclipse.dataspaceconnector.iam.did.resolver;

import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.RsaPublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * TODO HACKATHON-1 TASK 6B This implementation needs to resolve the key by resolving the DID and loading the public key contained in the DID document.
 */
public class DidPublicKeyResolverImpl implements DidPublicKeyResolver {
    private final PublicKey publicKey;

    public DidPublicKeyResolverImpl(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public PublicKeyWrapper resolvePublicKey(String did) {
        return new RsaPublicKeyWrapper((RSAPublicKey) publicKey);
    }
}

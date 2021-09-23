package org.eclipse.dataspaceconnector.iam.did.spi.resolver;

import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;

/**
 * Resolves a public key contained in a DID document associated with a DID.
 */
public interface DidPublicKeyResolver {

    /**
     * Resolves the public key. Note null is returned if the key cannot be resolved.
     */
    @Nullable
    PublicKey resolvePublicKey(String did);

}

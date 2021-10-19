package org.eclipse.dataspaceconnector.iam.did.spi.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a public key contained in a DID document associated with a DID.
 */
public interface DidPublicKeyResolver {

    String FEATURE = "edc:identity:public-key-resolver";

    /**
     * Resolves the public key. Note null is returned if the key cannot be resolved.
     */
    @Nullable
    PublicKeyWrapper resolvePublicKey(String did);

}

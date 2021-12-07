package org.eclipse.dataspaceconnector.iam.did.spi.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.result.Result;

/**
 * Resolves a public key contained in a DID document associated with a DID.
 */
public interface DidPublicKeyResolver {

    String FEATURE = "edc:identity:public-key-resolver";

    /**
     * Resolves the public key.
     */
    Result<PublicKeyWrapper> resolvePublicKey(String did);

}

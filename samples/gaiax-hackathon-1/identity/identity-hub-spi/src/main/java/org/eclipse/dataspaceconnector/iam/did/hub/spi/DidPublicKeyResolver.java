package org.eclipse.dataspaceconnector.iam.did.hub.spi;

import java.security.PublicKey;

/**
 *
 */
public interface DidPublicKeyResolver {

    PublicKey resolvePublicKey(String did);

}

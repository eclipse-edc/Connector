package org.eclipse.dataspaceconnector.iam.did.spi.resolver;

import java.security.PublicKey;

/**
 *
 */
public interface DidPublicKeyResolver {

    PublicKey resolvePublicKey(String did);

}

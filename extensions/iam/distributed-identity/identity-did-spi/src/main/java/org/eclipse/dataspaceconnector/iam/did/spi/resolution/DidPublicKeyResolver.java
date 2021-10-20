package org.eclipse.dataspaceconnector.iam.did.spi.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;

/**
 * Resolves a public key contained in a DID document associated with a DID.
 */
public interface DidPublicKeyResolver {

    String FEATURE = "edc:identity:public-key-resolver";

    /**
     * Resolves the public key.
     */
    Result resolvePublicKey(String did);

    /**
     * The response of a resolve operation.
     */
    class Result {
        private PublicKeyWrapper wrapper;
        private String invalidMessage;

        public Result(PublicKeyWrapper wrapper) {
            this.wrapper = wrapper;
        }

        public Result(String invalidMessage) {
            this.invalidMessage = invalidMessage;
        }

        public boolean invalid() {
            return invalidMessage != null;
        }

        public PublicKeyWrapper getWrapper() {
            return wrapper;
        }

        public String getInvalidMessage() {
            return invalidMessage;
        }
    }
}

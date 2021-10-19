package org.eclipse.dataspaceconnector.iam.did.resolver;

import org.eclipse.dataspaceconnector.iam.did.crypto.CryptoException;

public class PublicKeyResolutionException extends CryptoException {
    public PublicKeyResolutionException(String s) {
        super(s);
    }

    public PublicKeyResolutionException(Exception e) {
        super(e);
    }
}

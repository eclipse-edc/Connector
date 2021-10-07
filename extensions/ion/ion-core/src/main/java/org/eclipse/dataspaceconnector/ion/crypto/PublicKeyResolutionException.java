package org.eclipse.dataspaceconnector.ion.crypto;

import org.eclipse.dataspaceconnector.ion.IonCryptoException;

public class PublicKeyResolutionException extends IonCryptoException {
    public PublicKeyResolutionException(String s) {
        super(s);
    }

    public PublicKeyResolutionException(Exception e) {
        super(e);
    }
}

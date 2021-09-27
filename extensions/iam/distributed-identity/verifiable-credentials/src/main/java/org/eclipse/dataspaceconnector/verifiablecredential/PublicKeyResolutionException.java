package org.eclipse.dataspaceconnector.verifiablecredential;

public class PublicKeyResolutionException extends CryptoException {
    public PublicKeyResolutionException(String s) {
        super(s);
    }

    public PublicKeyResolutionException(Exception e) {
        super(e);
    }
}

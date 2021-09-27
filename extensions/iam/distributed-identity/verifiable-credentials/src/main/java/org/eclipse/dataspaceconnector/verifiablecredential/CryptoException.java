package org.eclipse.dataspaceconnector.verifiablecredential;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class CryptoException extends EdcException {
    public CryptoException(Exception inner) {
        super(inner);
    }

    public CryptoException() {
        super("Cyptographic Exception");
    }

    public CryptoException(String s) {
        super(s);
    }
}

package org.eclipse.dataspaceconnector.ion;

/**
 * An error occurred during a cryptography-related action
 */
public class IonCryptoException extends IonException {
    public IonCryptoException(String message) {
        super(message);
    }
}

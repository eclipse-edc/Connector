package org.eclipse.dataspaceconnector.ion;

/**
 * An error occurred during communication with ION
 */
public class IonRequestException extends IonException {
    public IonRequestException(String message) {
        super(message);
    }

    public IonRequestException(Exception ex) {
        super(ex);
    }
}

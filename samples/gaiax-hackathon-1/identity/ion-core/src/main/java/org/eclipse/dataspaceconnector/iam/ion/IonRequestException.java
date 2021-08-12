package org.eclipse.dataspaceconnector.iam.ion;

public class IonRequestException extends IonException {
    public IonRequestException(String message) {
        super(message);
    }

    public IonRequestException(Exception ex) {
        super(ex);
    }
}

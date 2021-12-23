package org.eclipse.dataspaceconnector.spi.system;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class EdcInjectionException extends EdcException {
    public EdcInjectionException(String s) {
        super(s);
    }

    public EdcInjectionException(Throwable e) {
        super(e);
    }
}

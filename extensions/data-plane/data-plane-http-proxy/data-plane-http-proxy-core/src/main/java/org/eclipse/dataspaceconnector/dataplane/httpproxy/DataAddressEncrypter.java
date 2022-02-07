package org.eclipse.dataspaceconnector.dataplane.httpproxy;

import org.eclipse.dataspaceconnector.dataplane.spi.token.DataEncrypter;

public class DataAddressEncrypter implements DataEncrypter {

    @Override
    public String encrypt(String raw) {
        return raw; //TODO
    }
}

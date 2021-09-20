package org.eclipse.dataspaceconnector.spi.security;

import java.security.PrivateKey;

public interface KeyParser {

    boolean canParse(Class<?> keyType);

    PrivateKey parse(String encoded);
}

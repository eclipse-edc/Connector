package org.eclipse.dataspaceconnector.spi.security;

public interface KeyParser<T> {

    boolean canParse(Class<?> keyType);

    T parse(String encoded);
}

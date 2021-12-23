package org.eclipse.dataspaceconnector.spi.security;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@FunctionalInterface
public interface PrivateKeyResolver {

    /**
     * Returns the private key associated with the id or null if not found.
     */
    @Nullable <T> T resolvePrivateKey(String id, Class<T> keyType);

    default <T> void addParser(KeyParser<T> parser) {
    }

    default <T> void addParser(Class<T> forType, Function<String, T> parseFunction) {
    }
}

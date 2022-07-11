/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.security;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public abstract class ConfigurablePrivateKeyResolver implements PrivateKeyResolver {

    private final List<KeyParser<?>> parsers;

    protected ConfigurablePrivateKeyResolver(KeyParser<?>... parsers) {
        this.parsers = Arrays.asList(parsers);
    }

    protected ConfigurablePrivateKeyResolver() {
        parsers = new ArrayList<>();
    }

    @Nullable
    protected abstract String getEncodedKey(String id);

    @Override
    public @Nullable <T> T resolvePrivateKey(String id, Class<T> keyType) {
        return Optional.ofNullable(getEncodedKey(id))
                .map(encoded -> keyType.cast(getParser(keyType).parse(encoded)))
                .orElse(null);
    }

    @Override
    public <T> void addParser(KeyParser<T> parser) {
        parsers.add(parser);
    }

    @Override
    public <T> void addParser(Class<T> forType, Function<String, T> parseFunction) {
        var p = new KeyParser<T>() {
            @Override
            public boolean canParse(Class<?> keyType) {
                return Objects.equals(keyType, forType);
            }

            @Override
            public T parse(String encoded) {
                return parseFunction.apply(encoded);
            }
        };
        addParser(p);
    }

    @SuppressWarnings("unchecked")
    private <T> KeyParser<T> getParser(Class<T> keyType) {
        return (KeyParser<T>) parsers.stream().filter(p -> p.canParse(keyType))
                .findFirst()
                .orElseThrow(() -> new EdcException("Cannot find KeyParser for type " + keyType));
    }
}

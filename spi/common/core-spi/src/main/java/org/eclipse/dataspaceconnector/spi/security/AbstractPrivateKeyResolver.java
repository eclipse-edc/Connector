/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.security;

import org.eclipse.dataspaceconnector.spi.EdcException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractPrivateKeyResolver implements PrivateKeyResolver {

    private final List<KeyParser<?>> parsers;

    public AbstractPrivateKeyResolver() {
        this.parsers = new ArrayList<>();
    }

    public AbstractPrivateKeyResolver(KeyParser<?>... parsers) {
        this.parsers = Arrays.asList(parsers);
    }

    @Override
    public <T> void addParser(KeyParser<T> parser) {
        parsers.add(parser);
    }

    @Override
    public <T> void addParser(Class<T> forType, Function<String, T> parseFunction) {
        var parser = new KeyParser<T>() {

            @Override
            public boolean canParse(Class<?> keyType) {
                return Objects.equals(keyType, forType);
            }

            @Override
            public T parse(String encoded) {
                return parseFunction.apply(encoded);
            }
        };
        addParser(parser);
    }

    @SuppressWarnings("unchecked")
    protected <T> KeyParser<T> getParser(Class<T> keyType) {
        return (KeyParser<T>) parsers.stream().filter(p -> p.canParse(keyType))
                .findFirst().orElseThrow(() -> {
                            throw new EdcException("Cannot find KeyParser for type " + keyType);
                        }
                );
    }
}

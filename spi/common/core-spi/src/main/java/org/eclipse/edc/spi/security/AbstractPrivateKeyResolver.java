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

package org.eclipse.edc.spi.security;

import org.eclipse.edc.spi.result.Result;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Not to be used anymore!
 *
 * @deprecated Please do not use or extend this class anymore, it is deprecated.
 */
@Deprecated(since = "0.4.1", forRemoval = true)
public abstract class AbstractPrivateKeyResolver implements PrivateKeyResolver {

    private final List<KeyParser> parsers;

    public AbstractPrivateKeyResolver() {
        this.parsers = new ArrayList<>();
    }

    public AbstractPrivateKeyResolver(KeyParser... parsers) {
        this.parsers = Arrays.asList(parsers);
    }

    @Override
    public <T> void addParser(KeyParser parser) {
        parsers.add(parser);
    }

    @Override
    public <T> void addParser(Class<T> forType, Function<String, T> parseFunction) {
        var parser = new KeyParser() {

            @Override
            public boolean canHandle(String encoded) {
                return Objects.equals(encoded, forType);
            }

            @Override
            public Result<PrivateKey> parse(String encoded) {
                throw new UnsupportedOperationException("This class is not supported anymore and will be removed soon!");
            }
        };
        addParser(parser);
    }

    @SuppressWarnings("unchecked")
    protected <T> KeyParser getParser(Class<T> keyType) {
        throw new UnsupportedOperationException("This class is not supported anymore and will be removed soon!");
    }
}

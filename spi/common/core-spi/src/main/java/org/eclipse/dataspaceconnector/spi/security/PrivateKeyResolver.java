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

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Resolves security keys by type.
 */
@FunctionalInterface
public interface PrivateKeyResolver {

    /**
     * Returns the private key associated with the id or null if not found.
     */
    @Nullable <T> T resolvePrivateKey(String id, Class<T> keyType);

    /**
     * Registers a parser for the key type.
     */
    default <T> void addParser(KeyParser<T> parser) {
    }

    /**
     * Registers a functional parser for the key type.
     */
    default <T> void addParser(Class<T> forType, Function<String, T> parseFunction) {
    }
}

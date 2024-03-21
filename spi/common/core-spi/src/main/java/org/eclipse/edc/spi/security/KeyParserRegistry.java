/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.security;

import org.eclipse.edc.spi.result.Result;

import java.security.Key;
import java.security.PrivateKey;

/**
 * Registry that holds multiple {@link KeyParser} instances that are used to deserialize a private key from their
 * at-rest representation.
 */
public interface KeyParserRegistry {
    /**
     * Register a {@link KeyParser}
     */
    void register(KeyParser parser);

    /**
     * Attempts to parse the String representation of a private key into a {@link PrivateKey}. If no parser can handle
     * the encoded format, or it is corrupt etc. then a failure is returned.
     *
     * @param encoded The private key in encoded format (PEM, OpenSSH, JWK, PKCS8,...)
     * @return a success result containing the private key, a failure if the encoded private key could not be deserialized.
     */
    Result<Key> parse(String encoded);
}

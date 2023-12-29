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

/**
 * Handles the parsing of serialized security keys of a give type.
 */
public interface KeyParser {

    /**
     * Returns true if this parser can deserialize the string representation of the private key.
     */
    boolean canHandle(String encoded);

    /**
     * Parses the encoded private key. If the encoded string is invalid, or the parser can't handle the input,
     * it must return a {@link Result#failure(String)}, it must never throw an exception.
     */
    Result<PrivateKey> parse(String encoded);
}

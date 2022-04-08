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

/**
 * Handles the parsing of serialized security keys of a give type.
 */
public interface KeyParser<T> {

    /**
     * Returns true if this parser can deserialize the key type.
     */
    boolean canParse(Class<?> keyType);

    /**
     * Deserialized the security key.
     */
    T parse(String encoded);
}

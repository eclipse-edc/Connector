/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.spi;

/**
 * ID / URI parser for IDS resources.
 */
public class IdsIdParser {
    public static final String SCHEME = "urn";
    public static final String DELIMITER = ":";

    public static IdsId parse(String urn) {
        if (urn == null) {
            throw new IllegalArgumentException("urn must not be null");
        }
        String[] parts = urn.split(DELIMITER, 3);

        String scheme = parts[0];
        if (parts.length < 3 || !scheme.equalsIgnoreCase(SCHEME)) {
            throw new IllegalArgumentException(String.format("Unexpected scheme: %s", scheme));
        }
        String typeString = parts[1];
        IdsType type = IdsType.fromValue(typeString);

        String idValue = parts[2];

        return IdsId.Builder.newInstance().type(type).value(idValue).build();
    }
}

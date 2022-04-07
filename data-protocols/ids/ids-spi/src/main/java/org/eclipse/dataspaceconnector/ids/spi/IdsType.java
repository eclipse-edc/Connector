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
 * ID / URI types for IDS resources.
 */
public enum IdsType {
    CONTRACT("contract"),
    CONTRACT_OFFER("contractoffer"),
    CONTRACT_REQUEST("contractrequest"),
    CONTRACT_AGREEMENT("contractagreement"),
    CONNECTOR("connector"),
    CATALOG("catalog"),
    ARTIFACT("artifact"),
    REPRESENTATION("representation"),
    RESOURCE("resource"),
    MEDIA_TYPE("mediatype"),
    PARTICIPANT("participant"),
    PERMISSION("permission"),
    CONSTRAINT("constraint"),
    PROHIBITION("prohibition"),
    MESSAGE("message");

    private final String value;

    IdsType(String value) {
        this.value = value;
    }

    public static IdsType fromValue(String value) {
        for (IdsType es : IdsType.values()) {
            if (es.value.equalsIgnoreCase(value)) {
                return es;
            }
        }
        throw new IllegalArgumentException(String.format("Unexpected nid: %s", value));
    }

    public String getValue() {
        return value;
    }
}

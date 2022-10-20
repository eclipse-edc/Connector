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
 *       Fraunhofer Institute for Software and Systems Engineering - clean up & add message type
 *
 */

package org.eclipse.edc.protocol.ids.spi.types;

/**
 * ID / URI types for IDS resources.
 */
public enum IdsType {
    ARTIFACT("artifact"),
    CATALOG("catalog"),
    CONNECTOR("connector"),
    CONSTRAINT("constraint"),
    CONTRACT_AGREEMENT("contractagreement"),
    CONTRACT_OFFER("contractoffer"),
    CONTRACT_REQUEST("contractrequest"),
    MEDIA_TYPE("mediatype"),
    MESSAGE("message"),
    OBLIGATION("obligation"),
    PARTICIPANT("participant"),
    PERMISSION("permission"),
    PROHIBITION("prohibition"),
    REPRESENTATION("representation"),
    RESOURCE("resource");

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

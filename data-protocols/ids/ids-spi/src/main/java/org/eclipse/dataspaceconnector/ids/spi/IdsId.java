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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.spi;

import java.net.URI;

/**
 * ID / URI generator for IDS resources.
 */
@Deprecated // This functionality will be moved to a transformer class
public class IdsId {
    private static final String SCHEME = "urn";
    private static final String DELIMITER = ":";

    private final Type type;
    private final String value;

    public IdsId(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public static IdsId message(String value) {
        return new IdsId(Type.MESSAGE, value);
    }

    public static IdsId participant(String value) {
        return new IdsId(Type.PARTICIPANT, value);
    }

    public static IdsId connector(String value) {
        return new IdsId(Type.CONNECTOR, value);
    }

    public static IdsId representation(String value) {
        return new IdsId(Type.REPRESENTATION, value);
    }

    public static IdsId resource(String value) {
        return new IdsId(Type.RESOURCE, value);
    }

    public static IdsId resourceCatalog(String value) {
        return new IdsId(Type.RESOURCE_CATALOG, value);
    }

    public static IdsId artifact(String value) {
        return new IdsId(Type.ARTIFACT, value);
    }

    public static IdsId fromUri(URI uri) {
        return parse(uri.getScheme() + DELIMITER + uri.getSchemeSpecificPart());
    }

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
        Type type = Type.fromValue(typeString);

        String idValue = parts[2];

        return new IdsId(type, idValue);
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public URI toUri() {
        return URI.create(String.join(DELIMITER, SCHEME, type.getValue(), value));
    }

    public String toString() {
        return String.join(DELIMITER, SCHEME, type.getValue(), value);
    }

    public enum Type {
        CONTRACT("contract"),
        CONTRACT_OFFER("contractoffer"),
        CONNECTOR("connector"),
        CATALOG("catalog"),
        ARTIFACT("artifact"),
        REPRESENTATION("representation"),
        RESOURCE("resource"),
        RESOURCE_CATALOG("resource_catalog"),
        MEDIA_TYPE("mediatype"),
        PARTICIPANT("participant"),
        MESSAGE("message");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type fromValue(String value) {
            for (Type es : Type.values()) {
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
}

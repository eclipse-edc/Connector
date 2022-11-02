/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH, Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring, move parser methods
 *
 */

package org.eclipse.edc.protocol.ids.spi.types;

import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;

/**
 * ID / URI generator for IDS objects.
 */
public class IdsId {
    public static final String SCHEME = "urn";
    public static final String DELIMITER = ":";

    private final IdsType type;
    private final String value;

    private IdsId(@NotNull IdsType type, @NotNull String value) {
        this.type = type;
        this.value = value;
    }

    public IdsType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public static class Builder {
        private IdsType type;
        private String value;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder type(IdsType idsType) {
            this.type = idsType;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder value(Integer value) {
            this.value = String.valueOf(value);
            return this;
        }

        public IdsId build() {
            return new IdsId(type, value);
        }
    }

    /**
     * Converts an IDS id to a URI using a given delimiter and scheme.
     *
     * @return the object as URI.
     */
    public URI toUri() {
        return URI.create(String.join(DELIMITER, SCHEME, this.getType().getValue(), this.getValue()));
    }

    /**
     * Converts an IDS id to a URI and returns it as a string.
     *
     * @return the IDS id as URI string.
     */
    @Override
    public String toString() {
        return toUri().toString();
    }

    /**
     * Builds an IDS id from a given URI.
     *
     * @param uri The well-formatted URI (e.g. "urn:artifact:id12345").
     * @return the IDS id object wrapped in a result object.
     */
    public static Result<IdsId> from(URI uri) {
        try {
            return from(uri.getScheme() + DELIMITER + uri.getSchemeSpecificPart());
        } catch (NullPointerException e) {
            return Result.failure("Could not parse from null value");
        }
    }

    /**
     * Builds an IDS id from a given String.
     *
     * @param urn The well-formatted String (e.g. "urn:artifact:id12345").
     * @return the IDS id object wrapped in a result object.
     */
    public static Result<IdsId> from(String urn) {
        if (urn == null) {
            return Result.failure("String must not be null");
        }

        var parts = urn.split(DELIMITER, 3);

        var scheme = parts[0];
        if (parts.length < 3 || !scheme.equalsIgnoreCase(SCHEME)) {
            return Result.failure(String.format("Unexpected scheme: %s", scheme));
        }

        IdsType type;
        try {
            type = IdsType.fromValue(parts[1]);
        } catch (IllegalArgumentException e) {
            return Result.failure("Could built a valid ids type");
        }

        var value = parts[2];

        return Result.success(IdsId.Builder.newInstance().type(type).value(value).build());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IdsId)) {
            return false;
        }
        IdsId id = (IdsId) obj;
        return this.value.equals(id.value) && this.type == id.type;
    }
}

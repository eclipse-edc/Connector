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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * ID / URI generator for IDS resources.
 */
public class IdsId {
    private final IdsType type;
    private final String value;

    private IdsId(@NotNull IdsType type, @NotNull String value) {
        this.type = Objects.requireNonNull(type);
        this.value = Objects.requireNonNull(value);
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

        public Builder value(int value) {
            this.value = String.valueOf(value);
            return this;
        }

        public IdsId build() {
            return new IdsId(type, value);
        }
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

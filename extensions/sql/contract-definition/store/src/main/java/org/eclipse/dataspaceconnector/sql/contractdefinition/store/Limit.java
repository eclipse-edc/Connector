/*
 *  Copyright (c) 2022 Daimler TSS GmbH
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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

import org.jetbrains.annotations.Nullable;

class Limit {
    private final Integer limit;
    private final Integer offset;

    private Limit(@Nullable Integer limit, @Nullable Integer offset) {
        if (limit != null && limit < 0) {
            throw new IllegalArgumentException("Limit must be a non-negative integer");
        }
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("Offset must be a non-negative integer");
        }
        this.limit = limit;
        this.offset = offset;
    }

    public String getStatement() {
        var stringBuilder = new StringBuilder();
        if (limit != null) {
            stringBuilder.append("LIMIT");
            stringBuilder.append(" ");
            stringBuilder.append(limit);
            if (offset != null) {
                stringBuilder.append(" ");
                stringBuilder.append("OFFSET");
                stringBuilder.append(" ");
                stringBuilder.append(offset);
            }
        }
        return stringBuilder.toString();
    }

    public static class Builder {
        private Integer limit;
        private Integer offset;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public Limit build() {
            return new Limit(limit, offset);
        }
    }
}
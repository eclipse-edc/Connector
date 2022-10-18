/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import static java.lang.String.format;

/**
 * Represents in a structural way an OFFSET clause in an SQL statement.
 * OFFSET clauses are optional in SQL statements, so if no OFFSET is required, simply pass {@code null} in the constructor.
 */
class OffsetClause implements Clause {

    private final Integer offset;

    OffsetClause(Integer offset) {
        this.offset = offset;
    }

    OffsetClause() {
        this(null);
    }

    @Override
    public String asString() {
        return offset != null ? format("OFFSET %d", offset) : "";
    }
}

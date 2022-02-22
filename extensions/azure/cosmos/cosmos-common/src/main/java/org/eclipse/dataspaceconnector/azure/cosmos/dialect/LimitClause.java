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

/**
 * Represents in a structural way an LIMIT clause in an SQL statement.
 * LIMIT clauses are optional in SQL statements, so if no LIMIT is required, simply pass {@code null} in the constructor.
 */
class LimitClause implements Clause {

    private final Integer limit;

    public LimitClause(Integer limit) {
        this.limit = limit;
    }

    LimitClause() {
        this(null);
    }

    @Override
    public String asString() {
        return limit != null ? "LIMIT " + limit : "";
    }
}

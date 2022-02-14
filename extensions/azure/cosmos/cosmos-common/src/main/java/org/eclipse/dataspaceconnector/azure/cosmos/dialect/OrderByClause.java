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

import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

/**
 * Represents in a structural way an ORDER BY clause in an SQL statement.
 * The sort criterion, i.e. the SQL column by which to sort, is needed.
 * <p>
 * Optionally an {@code orderPrefix} can be specified, which represents the "path" of the property. This is particularly
 * relevant for CosmosDB queries, e.g:
 * <pre>
 *     SELECT * FROM YourDocument WHERE ... ORDER BY YourDocument.Header.Age
 * </pre>
 * In this case the {@code orderPrefix} would have to be {@code "YourDocument.Header"}.
 */
class OrderByClause implements Clause {

    private final String orderField;
    private final boolean sortAsc;
    private final String objectPrefix;

    public OrderByClause(String orderField, boolean sortAsc, String objectPrefix) {

        this.orderField = orderField;
        this.sortAsc = sortAsc;
        this.objectPrefix = objectPrefix;
    }

    public OrderByClause(String orderField, boolean sortAsc) {
        this(orderField, sortAsc, null);
    }

    OrderByClause() {
        this(null, true, null);
    }

    @Override
    public String asString() {
        return orderField != null ? format("ORDER BY %s%s %s", getPrefix(), orderField, sortAsc ? "ASC" : "DESC") : "";
    }

    @NotNull
    private String getPrefix() {
        return objectPrefix != null ? objectPrefix + "." : "";
    }

}

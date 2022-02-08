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

class OrderByClause implements Clause {

    private final String orderField;
    private final boolean sortAsc;
    private final String objectPrefix;

    public OrderByClause(String orderField, boolean sortAsc, String objectPrefix) {

        this.orderField = orderField;
        this.sortAsc = sortAsc;
        this.objectPrefix = objectPrefix;
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

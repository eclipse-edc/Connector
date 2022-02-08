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

package org.eclipse.dataspaceconnector.spi.query;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Specifies various query parameters for collection-like queries.
 * Typical uses include API endpoints, where the query is tunnelled through to the database level.
 */
public class QuerySpec {
    private int offset = 0;
    private int limit = 50;
    private List<Criterion> filterExpression;
    private SortOrder sortOrder = SortOrder.DESC;
    private String sortField;

    public String getSortField() {
        return sortField;
    }

    @Override
    public String toString() {
        return "QuerySpec{" +
                "offset=" + offset +
                ", pageSize=" + limit +
                ", filterExpression=" + filterExpression +
                ", sortOrder=" + sortOrder +
                ", sortField=" + sortField +
                '}';
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public List<Criterion> getFilterExpression() {
        return filterExpression;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public static final class Builder {
        private final QuerySpec querySpec;

        private Builder() {
            querySpec = new QuerySpec();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder offset(int offset) {
            querySpec.offset = offset;
            return this;
        }

        public Builder limit(int limit) {
            querySpec.limit = limit;
            return this;
        }

        public Builder sortOrder(SortOrder sortOrder) {
            querySpec.sortOrder = sortOrder;
            return this;
        }

        public Builder sortField(String sortField) {
            querySpec.sortField = sortField;
            return this;
        }

        public QuerySpec build() {
            if (querySpec.offset < 0) {
                throw new IllegalArgumentException("offset");
            }
            if (querySpec.limit <= 0) {
                throw new IllegalArgumentException("limit");
            }
            return querySpec;
        }

        public Builder filter(String filterExpression) {

            if (filterExpression != null) {
                if (Pattern.matches("[^\\s\\\\]*(\\s*)=(\\s*)[^\\s\\\\]*", filterExpression)) { // something like X = Y
                    // remove whitespaces
                    filterExpression = filterExpression.replace(" ", "");
                    // we'll interpret the "=" as "contains"
                    var tokens = filterExpression.split("=");
                    querySpec.filterExpression = List.of(new Criterion(tokens[0], "contains", tokens[1]));
                } else {
                    var sanitized = filterExpression.replaceAll(" +", " ");
                    var s = sanitized.split(" ");

                    //generic LEFT OPERAND RIGHT expression
                    if (s.length == 3) {
                        querySpec.filterExpression = List.of(new Criterion(s[0], s[1], s[2]));
                    } else {
                        // unsupported filter expression
                        throw new IllegalArgumentException("Cannot convert " + filterExpression + " into a Criterion");
                    }
                }
            }

            return this;
        }
    }
}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Specifies various query parameters for collection-like queries. Typical uses include API endpoints, where the query
 * is tunnelled through to the database level.
 */
public class QuerySpec {
    private int offset = 0;
    private int limit = 50;
    private List<Criterion> filterExpression = new ArrayList<>();
    private SortOrder sortOrder = SortOrder.ASC;
    private String sortField;

    public static QuerySpec none() {
        return new QuerySpec();
    }

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
        private static final String EQUALS_EXPRESSION_PATTERN = "[^\\s\\\\]*(\\s*)=(\\s*)[^\\\\]*";
        private final QuerySpec querySpec;
        private boolean equalsAsContains = false;

        private Builder() {
            querySpec = new QuerySpec();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder offset(Integer offset) {
            if (offset != null) {
                querySpec.offset = offset;
            }
            return this;
        }

        public Builder limit(Integer limit) {
            if (limit != null) {
                querySpec.limit = limit;
            }
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

        public Builder equalsAsContains(boolean equalsAsContains) {
            this.equalsAsContains = equalsAsContains;
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

        public Builder filter(List<Criterion> criteria) {
            if (criteria != null) {
                querySpec.filterExpression = criteria;
            }
            return this;
        }

        public Builder filter(String filterExpression) {

            if (filterExpression != null) {
                if (Pattern.matches(EQUALS_EXPRESSION_PATTERN, filterExpression)) { // something like X = Y
                    // we'll interpret the "=" as "contains" if desired
                    var tokens = filterExpression.split("=", 2);
                    var left = tokens[0].trim();
                    var right = tokens[1].trim();
                    var op = equalsAsContains ? "contains" : "=";
                    querySpec.filterExpression = List.of(new Criterion(left, op, right));
                } else {
                    var s = filterExpression.split(" +", 3);

                    //generic LEFT OPERAND RIGHT expression
                    if (s.length >= 3) {
                        var rh = Arrays.stream(s, 2, s.length).collect(Collectors.joining(" "));
                        querySpec.filterExpression = List.of(new Criterion(s[0], s[1], rh));
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

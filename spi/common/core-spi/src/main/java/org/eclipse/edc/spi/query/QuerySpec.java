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

package org.eclipse.edc.spi.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.edc.spi.message.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Specifies various query parameters for collection-like queries. Typical uses include API endpoints, where the query
 * is tunnelled through to the database level.
 */
public class QuerySpec {

    public static final String EDC_QUERY_SPEC_TYPE_TERM = "QuerySpec";
    public static final String EDC_QUERY_SPEC_TYPE = EDC_NAMESPACE + EDC_QUERY_SPEC_TYPE_TERM;
    public static final String EDC_QUERY_SPEC_OFFSET = EDC_NAMESPACE + "offset";
    public static final String EDC_QUERY_SPEC_LIMIT = EDC_NAMESPACE + "limit";
    public static final String EDC_QUERY_SPEC_FILTER_EXPRESSION = EDC_NAMESPACE + "filterExpression";
    public static final String EDC_QUERY_SPEC_SORT_ORDER = EDC_NAMESPACE + "sortOrder";
    public static final String EDC_QUERY_SPEC_SORT_FIELD = EDC_NAMESPACE + "sortField";
    private final List<Criterion> filterExpression = new ArrayList<>();
    private int offset = 0;
    private int limit = 50;
    private SortOrder sortOrder = SortOrder.ASC;
    private String sortField;

    public static QuerySpec none() {
        return new QuerySpec();
    }

    /**
     * Generates a {@link QuerySpec} with limit = {@link Integer#MAX_VALUE} and offset = 0
     */
    public static QuerySpec max() {
        return QuerySpec.Builder.newInstance().limit(Integer.MAX_VALUE).build();
    }

    public String getSortField() {
        return sortField;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    @JsonIgnore
    public Range getRange() {
        return new Range(offset, offset + limit);
    }

    public List<Criterion> getFilterExpression() {
        return filterExpression;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
     * Checks whether any {@link Criterion} contains the given left-hand operand
     */
    public boolean containsAnyLeftOperand(String leftOperand) {
        return getFilterExpression().stream().anyMatch(c -> c.getOperandLeft().toString().startsWith(leftOperand));
    }

    public Builder toBuilder() {
        return new Builder()
                .offset(offset)
                .limit(limit)
                .filter(filterExpression)
                .sortOrder(sortOrder)
                .sortField(sortField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, limit, filterExpression, sortOrder, sortField);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuerySpec querySpec = (QuerySpec) o;
        return offset == querySpec.offset && limit == querySpec.limit && Objects.equals(filterExpression, querySpec.filterExpression) && sortOrder == querySpec.sortOrder && Objects.equals(sortField, querySpec.sortField);
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


    public static final class Builder {
        private final QuerySpec querySpec;

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

        public Builder range(Range range) {
            offset(range.getFrom());
            limit(range.getTo() - range.getFrom());
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

        public Builder filter(Criterion criterion) {
            querySpec.filterExpression.add(criterion);
            return this;
        }

        public Builder filter(List<Criterion> criteria) {
            if (criteria != null) {
                querySpec.filterExpression.addAll(criteria);
            }
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


    }
}

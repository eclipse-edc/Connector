/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.query;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.QueryParam;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

public class QuerySpecDto {

    @QueryParam("offset")
    @PositiveOrZero
    private Integer offset = 0;

    @QueryParam("limit")
    @Positive
    private Integer limit = 50;

    @QueryParam("filter")
    private String filter;

    @QueryParam("sort")
    private SortOrder sortOrder = SortOrder.ASC;

    @QueryParam("sortField")
    private String sortField;

    public QuerySpecDto() {

    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public String getFilter() {
        return filter;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public String getSortField() {
        return sortField;
    }

    @AssertTrue
    public boolean isValid() {
        if (filter != null && filter.isBlank()) {
            return false;
        }

        if (sortField != null && sortField.isBlank()) {
            return false;
        }

        return true;
    }

    public static final class Builder {
        private final QuerySpecDto querySpec;

        private Builder() {
            querySpec = new QuerySpecDto();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder offset(Integer offset) {
            querySpec.offset = offset;
            return this;
        }

        public Builder limit(Integer limit) {
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

        public Builder filter(String filter) {
            querySpec.filter = filter;
            return this;
        }

        public QuerySpecDto build() {
            return querySpec;
        }

    }
}

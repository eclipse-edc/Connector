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

package org.eclipse.edc.api.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.QueryParam;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.spi.query.SortOrder;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(builder = QuerySpecDto.Builder.class)
public class QuerySpecDto {

    @QueryParam("offset")
    @PositiveOrZero(message = "offset must be greater or equal to zero")
    private Integer offset = 0;

    @QueryParam("limit")
    @Positive(message = "limit must be greater than 0")
    private Integer limit = 50;

    private List<CriterionDto> filterExpression = new ArrayList<>();

    @QueryParam("sort")
    private SortOrder sortOrder = SortOrder.ASC;

    @QueryParam("sortField")
    private String sortField;

    public Integer getOffset() {
        return offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public String getSortField() {
        return sortField;
    }

    public List<CriterionDto> getFilterExpression() {
        return filterExpression;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final QuerySpecDto querySpec;

        private Builder() {
            querySpec = new QuerySpecDto();
        }

        @JsonCreator
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

        public Builder filterExpression(List<CriterionDto> filterExpression) {
            querySpec.filterExpression = filterExpression;
            return this;
        }

        public QuerySpecDto build() {
            return querySpec;
        }

    }
}

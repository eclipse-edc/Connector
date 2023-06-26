/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.query.SortOrder;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

@JsonDeserialize(builder = QuerySpecDto.Builder.class)
public class QuerySpecDto extends BaseDto {

    public static final String EDC_QUERY_SPEC_TYPE = EDC_NAMESPACE + "QuerySpecDto";
    public static final String EDC_QUERY_SPEC_OFFSET = EDC_NAMESPACE + "offset";
    public static final String EDC_QUERY_SPEC_LIMIT = EDC_NAMESPACE + "limit";
    public static final String EDC_QUERY_SPEC_FILTER_EXPRESSION = EDC_NAMESPACE + "filterExpression";
    public static final String EDC_QUERY_SPEC_SORT_ORDER = EDC_NAMESPACE + "sortOrder";
    public static final String EDC_QUERY_SPEC_SORT_FIELD = EDC_NAMESPACE + "sortField";

    private Integer offset = 0;
    private Integer limit = 50;
    private SortOrder sortOrder = SortOrder.ASC;
    private String sortField;
    private final List<CriterionDto> filterExpression = new ArrayList<>();

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

        public Builder filter(CriterionDto criterion) {
            querySpec.filterExpression.add(criterion);
            return this;
        }

        public Builder filterExpression(List<CriterionDto> filterExpression) {
            querySpec.filterExpression.addAll(filterExpression);
            return this;
        }

        public QuerySpecDto build() {
            return querySpec;
        }

    }
}

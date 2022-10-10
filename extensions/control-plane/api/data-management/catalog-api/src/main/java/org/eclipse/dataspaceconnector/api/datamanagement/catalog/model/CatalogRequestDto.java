/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.datamanagement.catalog.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.eclipse.dataspaceconnector.api.model.CriterionDto;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@JsonDeserialize(builder = CatalogRequestDto.Builder.class)
public class CatalogRequestDto {
    @PositiveOrZero(message = "offset must be greater or equal to zero")
    private Integer offset = 0;
    @Positive(message = "limit must be greater than 0")
    private Integer limit = 50;
    private SortOrder sortOrder = SortOrder.ASC;
    private List<CriterionDto> filter = new ArrayList<>();
    private String sortField;
    @NotNull
    private String providerUrl;

    private CatalogRequestDto() {
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public List<CriterionDto> getFilter() {
        return filter;
    }

    public String getSortField() {
        return sortField;
    }

    public String getProviderUrl() {
        return providerUrl;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final CatalogRequestDto instance;

        private Builder() {
            instance = new CatalogRequestDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder offset(Integer offset) {
            instance.offset = offset;
            return this;
        }

        public Builder limit(Integer limit) {
            instance.limit = limit;
            return this;
        }

        public Builder sortOrder(SortOrder sortOrder) {
            instance.sortOrder = sortOrder;
            return this;
        }

        public Builder providerUrl(String providerUrl) {
            instance.providerUrl = providerUrl;
            return this;
        }

        public Builder filter(List<CriterionDto> criteria) {
            instance.filter = criteria;
            return this;
        }

        public Builder sortField(String sortField) {
            instance.sortField = sortField;
            return this;
        }

        public CatalogRequestDto build() {
            requireNonNull(instance.providerUrl, "providerUrl");
            return instance;
        }
    }
}

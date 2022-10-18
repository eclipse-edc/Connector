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
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;

import static java.util.Objects.requireNonNull;

@JsonDeserialize(builder = CatalogRequestDto.Builder.class)
public class CatalogRequestDto {

    private QuerySpecDto querySpec;
    @NotNull
    private String providerUrl;

    private CatalogRequestDto() {
    }


    public String getProviderUrl() {
        return providerUrl;
    }

    public QuerySpecDto getQuerySpec() {
        return querySpec;
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

        public Builder querySpec(QuerySpecDto querySpecDto) {
            instance.querySpec = querySpecDto;
            return this;
        }

        public Builder providerUrl(String providerUrl) {
            instance.providerUrl = providerUrl;
            return this;
        }

        public CatalogRequestDto build() {
            requireNonNull(instance.providerUrl, "providerUrl");
            return instance;
        }
    }
}

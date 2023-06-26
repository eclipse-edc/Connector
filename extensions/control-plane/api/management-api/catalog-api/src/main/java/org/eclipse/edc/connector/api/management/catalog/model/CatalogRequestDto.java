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

package org.eclipse.edc.connector.api.management.catalog.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.api.model.BaseDto;
import org.eclipse.edc.api.model.QuerySpecDto;

import static java.util.Objects.requireNonNull;

@JsonDeserialize(builder = CatalogRequestDto.Builder.class)
public class CatalogRequestDto extends BaseDto {

    private QuerySpecDto querySpec;
    private String providerUrl;
    private String protocol;

    private CatalogRequestDto() {
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public QuerySpecDto getQuerySpec() {
        return querySpec;
    }

    public String getProtocol() {
        return protocol;
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

        public Builder protocol(String protocol) {
            instance.protocol = protocol;
            return this;
        }

        public CatalogRequestDto build() {
            requireNonNull(instance.providerUrl, "providerUrl");
            return instance;
        }
    }
}

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

package org.eclipse.edc.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.query.QuerySpec;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class CatalogRequest {

    public static final String CATALOG_REQUEST_TYPE = EDC_NAMESPACE + "CatalogRequest";
    public static final String CATALOG_REQUEST_PROTOCOL = EDC_NAMESPACE + "protocol";
    public static final String CATALOG_REQUEST_PROVIDER_URL = EDC_NAMESPACE + "providerUrl";
    public static final String CATALOG_REQUEST_QUERY_SPEC = EDC_NAMESPACE + "querySpec";

    private QuerySpec querySpec;
    private String providerUrl;
    private String protocol;

    private CatalogRequest() {
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public QuerySpec getQuerySpec() {
        return querySpec;
    }

    public String getProtocol() {
        return protocol;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final CatalogRequest instance;

        private Builder() {
            instance = new CatalogRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder querySpec(QuerySpec querySpecDto) {
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

        public CatalogRequest build() {
            requireNonNull(instance.providerUrl, "providerUrl");
            return instance;
        }
    }
}

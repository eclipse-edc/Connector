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

package org.eclipse.edc.connector.dataplane.http.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.HTTP_DATA_TYPE;

/**
 * This is a wrapper class for the {@link DataAddress} object, which has typed accessors for properties specific to
 * a http endpoint.
 */
@JsonTypeName()
@JsonDeserialize(builder = DataAddress.Builder.class)
public class HttpDataAddress extends DataAddress {

    private static final String NAME = "name";
    private static final String PATH = "path";
    private static final String QUERY_PARAMS = "queryParams";
    private static final String METHOD = "method";
    private static final String AUTH_KEY = "authKey";
    private static final String AUTH_CODE = "authCode";
    private static final String SECRET_NAME = "secretName";
    @Deprecated(since = "0.14.0")
    private static final String PROXY_BODY = "proxyBody";
    @Deprecated(since = "0.14.0")
    private static final String PROXY_PATH = "proxyPath";
    @Deprecated(since = "0.14.0")
    private static final String PROXY_QUERY_PARAMS = "proxyQueryParams";
    @Deprecated(since = "0.14.0")
    private static final String PROXY_METHOD = "proxyMethod";
    public static final String ADDITIONAL_HEADER = "header:";
    public static final String CONTENT_TYPE = "contentType";
    public static final String OCTET_STREAM = "application/octet-stream";
    public static final String NON_CHUNKED_TRANSFER = "nonChunkedTransfer";
    public static final Set<String> ADDITIONAL_HEADERS_TO_IGNORE = Set.of("content-type");

    private HttpDataAddress() {
        super();
        this.setType(HTTP_DATA_TYPE);
    }

    @JsonIgnore
    public String getName() {
        return getStringProperty(NAME);
    }

    @JsonIgnore
    public String getBaseUrl() {
        return getStringProperty(BASE_URL);
    }

    @JsonIgnore
    public String getPath() {
        return getStringProperty(PATH);
    }

    @JsonIgnore
    public String getQueryParams() {
        return getStringProperty(QUERY_PARAMS);
    }

    @JsonIgnore
    public String getMethod() {
        return getStringProperty(METHOD);
    }

    @JsonIgnore
    public String getAuthKey() {
        return getStringProperty(AUTH_KEY);
    }

    @JsonIgnore
    public String getAuthCode() {
        return getStringProperty(AUTH_CODE);
    }

    @JsonIgnore
    public String getSecretName() {
        return getStringProperty(SECRET_NAME);
    }

    @JsonIgnore
    @Deprecated(since = "0.14.0")
    public String getProxyBody() {
        return getStringProperty(PROXY_BODY);
    }

    @JsonIgnore
    @Deprecated(since = "0.14.0")
    public String getProxyPath() {
        return getStringProperty(PROXY_PATH);
    }

    @JsonIgnore
    @Deprecated(since = "0.14.0")
    public String getProxyQueryParams() {
        return getStringProperty(PROXY_QUERY_PARAMS);
    }

    @JsonIgnore
    @Deprecated(since = "0.14.0")
    public String getProxyMethod() {
        return getStringProperty(PROXY_METHOD);
    }

    @JsonIgnore
    public String getContentType() {
        return getStringProperty(CONTENT_TYPE, OCTET_STREAM);
    }

    @JsonIgnore
    public Map<String, String> getAdditionalHeaders() {
        return getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ADDITIONAL_HEADER))
                .collect(toMap(entry -> entry.getKey().replace(ADDITIONAL_HEADER, ""), it -> (String) it.getValue()));
    }

    @JsonIgnore
    public boolean getNonChunkedTransfer() {
        return Optional.of(NON_CHUNKED_TRANSFER)
                .map(this::getStringProperty)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends DataAddress.Builder<HttpDataAddress, Builder> {

        private Builder() {
            super(new HttpDataAddress());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            this.property(NAME, name);
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.property(BASE_URL, baseUrl);
            return this;
        }

        public Builder path(String path) {
            this.property(PATH, path);
            return this;
        }

        public Builder queryParams(String queryParams) {
            this.property(QUERY_PARAMS, queryParams);
            return this;
        }

        public Builder method(String method) {
            this.property(METHOD, method);
            return this;
        }

        public Builder authKey(String authKey) {
            this.property(AUTH_KEY, authKey);
            return this;
        }

        public Builder authCode(String authCode) {
            this.property(AUTH_CODE, authCode);
            return this;
        }

        public Builder secretName(String secretName) {
            this.property(SECRET_NAME, secretName);
            return this;
        }

        @Deprecated(since = "0.14.0")
        public Builder proxyBody(String proxyBody) {
            this.property(PROXY_BODY, proxyBody);
            return this;
        }

        @Deprecated(since = "0.14.0")
        public Builder proxyPath(String proxyPath) {
            this.property(PROXY_PATH, proxyPath);
            return this;
        }

        @Deprecated(since = "0.14.0")
        public Builder proxyQueryParams(String proxyQueryParams) {
            this.property(PROXY_QUERY_PARAMS, proxyQueryParams);
            return this;
        }

        @Deprecated(since = "0.14.0")
        public Builder proxyMethod(String proxyMethod) {
            this.property(PROXY_METHOD, proxyMethod);
            return this;
        }

        public Builder addAdditionalHeader(String additionalHeaderName, String additionalHeaderValue) {
            if (ADDITIONAL_HEADERS_TO_IGNORE.contains(additionalHeaderName.toLowerCase())) {
                return this;
            }

            address.getProperties().put(ADDITIONAL_HEADER + additionalHeaderName, Objects.requireNonNull(additionalHeaderValue));
            return this;
        }

        public Builder contentType(String contentType) {
            this.property(CONTENT_TYPE, contentType);
            return this;
        }

        public Builder nonChunkedTransfer(boolean nonChunkedTransfer) {
            this.property(NON_CHUNKED_TRANSFER, String.valueOf(nonChunkedTransfer));
            return this;
        }

        public Builder copyFrom(DataAddress other) {
            Optional.ofNullable(other).map(DataAddress::getProperties).orElse(emptyMap()).forEach(this::property);
            return this;
        }

        @Override
        public HttpDataAddress build() {
            this.type(HTTP_DATA_TYPE);
            return address;
        }
    }
}

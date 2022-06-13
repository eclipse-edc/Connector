/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This is a wrapper class for the {@link DataAddress} object, which has typed accessors for properties specific to
 * a http endpoint.
 */
@JsonTypeName()
@JsonDeserialize(builder = DataAddress.Builder.class)
public class HttpDataAddress extends DataAddress {

    public static final String DATA_TYPE = "HttpData";

    private static final String NAME = "name";
    private static final String BASE_URL = "baseUrl";
    private static final String AUTH_KEY = "authKey";
    private static final String AUTH_CODE = "authCode";
    private static final String SECRET_NAME = "secretName";
    private static final String PROXY_BODY = "proxyBody";
    private static final String PROXY_PATH = "proxyPath";
    private static final String PROXY_QUERY_PARAMS = "proxyQueryParams";
    private static final String PROXY_METHOD = "proxyMethod";

    private HttpDataAddress() {
        super();
    }

    @JsonIgnore
    public String getName() {
        return getProperty(NAME);
    }

    @JsonIgnore
    public String getBaseUrl() {
        return getProperty(BASE_URL);
    }

    @JsonIgnore
    public String getAuthKey() {
        return getProperty(AUTH_KEY);
    }

    @JsonIgnore
    public String getAuthCode() {
        return getProperty(AUTH_CODE);
    }

    @JsonIgnore
    public String getSecretName() {
        return getProperty(SECRET_NAME);
    }

    @JsonIgnore
    public String getProxyBody() {
        return getProperty(PROXY_BODY);
    }

    @JsonIgnore
    public String getProxyPath() {
        return getProperty(PROXY_PATH);
    }

    @JsonIgnore
    public String getProxyQueryParams() {
        return getProperty(PROXY_QUERY_PARAMS);
    }

    @JsonIgnore
    public String getProxyMethod() {
        return getProperty(PROXY_METHOD);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends DataAddress.Builder {

        private Builder() {
            super(new HttpDataAddress());
            this.type(DATA_TYPE);
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

        public Builder proxyBody(String proxyBody) {
            this.property(PROXY_BODY, proxyBody);
            return this;
        }

        public Builder proxyPath(String proxyPath) {
            this.property(PROXY_PATH, proxyPath);
            return this;
        }

        public Builder proxyQueryParams(String proxyQueryParams) {
            this.property(PROXY_QUERY_PARAMS, proxyQueryParams);
            return this;
        }

        public Builder proxyMethod(String proxyMethod) {
            this.property(PROXY_METHOD, proxyMethod);
            return this;
        }

        public Builder copyFrom(DataAddress other) {
            other.getProperties().forEach(this::property);
            return this;
        }

        @Override
        public HttpDataAddress build() {
            this.type(DATA_TYPE);
            return (HttpDataAddress) address;
        }
    }
}

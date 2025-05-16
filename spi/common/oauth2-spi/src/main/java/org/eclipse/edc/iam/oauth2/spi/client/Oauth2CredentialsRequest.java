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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.oauth2.spi.client;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class Oauth2CredentialsRequest {

    private static final String GRANT_TYPE = "grant_type";
    private static final String SCOPE = "scope";
    private static final String RESOURCE = "resource";

    protected String url;
    protected final Map<String, Object> params = new HashMap<>();

    @NotNull
    public String getUrl() {
        return url;
    }

    public Object getScope() {
        return params.get(SCOPE);
    }

    public Object getGrantType() {
        return params.get(GRANT_TYPE);
    }

    /**
     * The audience for which an access token will be requested.
     *
     * @return The value of the resource form parameter.
     */
    public Object getResource() {
        return this.params.get(RESOURCE);
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public abstract static class Builder<T extends Oauth2CredentialsRequest, B extends Builder<T, B>> {
        protected final T request;

        protected Builder(T entity) {
            this.request = entity;
        }

        public B url(String url) {
            request.url = url;
            return self();
        }

        public B grantType(String grantType) {
            param(GRANT_TYPE, grantType);
            return self();
        }

        public B scope(String scope) {
            param(SCOPE, scope);
            return self();
        }

        public B param(String key, Object value) {
            request.params.put(key, value);
            return self();
        }

        public B params(Map<String, Object> params) {
            request.params.putAll(params);
            return self();
        }

        /**
         * Adds the resource form parameter to the request.
         *
         * @param targetedAudience The audience for which an access token will be requested.
         * @return this builder
         * @see <a href="https://www.rfc-editor.org/rfc/rfc8707.html">RFC-8707</a>
         */
        public B resource(String targetedAudience) {
            return param(RESOURCE, targetedAudience);
        }

        public abstract B self();

        protected T build() {
            Objects.requireNonNull(request.url, "url");
            Objects.requireNonNull(request.params.get(GRANT_TYPE), GRANT_TYPE);
            return request;
        }

    }
}

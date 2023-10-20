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

package org.eclipse.edc.connector.api.sts.model;

import jakarta.ws.rs.FormParam;

/**
 * OAuth2 Client Credentials <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.4.2">Access Token Request</a>
 *
 * <ul>
 *  <li>grantType:          Type of grant. Must be client_credentials.</li>
 *  <li>clientId:           Client ID identifier.</li>
 *  <li>clientSecret:       Authorization secret for the client/</li>
 *  <li>audience:           Audience according to the <a href="https://datatracker.ietf.org/doc/html/draft-tschofenig-oauth-audience-00#section-3">spec</a>.</li>
 *  <li>bearerAccessScope:  Space-delimited scopes to be included in the access_token claim.</li>
 *  <li>accessToken:        VP/VC Access Token to be included as access_token claim.</li>
 *  <li>grantType:          Type of grant. Must be client_credentials.</li>
 * </ul>
 */

public final class StsTokenRequest {
    @FormParam("grant_type")
    private String grantType;
    @FormParam("client_id")
    private String clientId;
    @FormParam("audience")
    private String audience;
    @FormParam("bearer_access_scope")
    private String bearerAccessScope;
    @FormParam("access_token")
    private String accessToken;
    @FormParam("client_secret")
    private String clientSecret;

    public StsTokenRequest() {

    }

    public String getGrantType() {
        return grantType;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAudience() {
        return audience;
    }

    public String getBearerAccessScope() {
        return bearerAccessScope;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public static class Builder {

        private final StsTokenRequest request;

        protected Builder(StsTokenRequest request) {
            this.request = request;
        }

        public static Builder newInstance() {
            return new Builder(new StsTokenRequest());
        }

        public Builder grantType(String grantType) {
            this.request.grantType = grantType;
            return this;
        }

        public Builder clientId(String clientId) {
            this.request.clientId = clientId;
            return this;
        }

        public Builder audience(String audience) {
            this.request.audience = audience;
            return this;
        }

        public Builder bearerAccessScope(String bearerAccessScope) {
            this.request.bearerAccessScope = bearerAccessScope;
            return this;
        }

        public Builder accessToken(String accessToken) {
            this.request.accessToken = accessToken;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.request.clientSecret = clientSecret;
            return this;
        }

        public StsTokenRequest build() {
            return request;
        }
    }
}

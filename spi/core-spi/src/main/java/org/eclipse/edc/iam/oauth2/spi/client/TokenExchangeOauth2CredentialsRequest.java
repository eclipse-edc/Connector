/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.oauth2.spi.client;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * An OAuth2 Token Exchange request as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8693">RFC 8693</a>.
 * <p>
 * The subject token is the sole proof of identity towards the token exchange broker, therefore this request
 * deliberately does not carry any client authentication credentials.
 */
public class TokenExchangeOauth2CredentialsRequest extends Oauth2CredentialsRequest {

    public static final String GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    public static final String TOKEN_TYPE_JWT = "urn:ietf:params:oauth:token-type:jwt";

    private static final String SUBJECT_TOKEN = "subject_token";
    private static final String SUBJECT_TOKEN_TYPE = "subject_token_type";
    private static final String REQUESTED_TOKEN_TYPE = "requested_token_type";
    private static final String AUDIENCE = "audience";

    public String getSubjectToken() {
        return ofNullable(params.get(SUBJECT_TOKEN)).map(Object::toString).orElse(null);
    }

    public String getSubjectTokenType() {
        return ofNullable(params.get(SUBJECT_TOKEN_TYPE)).map(Object::toString).orElse(null);
    }

    public String getRequestedTokenType() {
        return ofNullable(params.get(REQUESTED_TOKEN_TYPE)).map(Object::toString).orElse(null);
    }

    public String getAudience() {
        return ofNullable(params.get(AUDIENCE)).map(Object::toString).orElse(null);
    }

    public static class Builder<B extends TokenExchangeOauth2CredentialsRequest.Builder<B>> extends Oauth2CredentialsRequest.Builder<TokenExchangeOauth2CredentialsRequest, TokenExchangeOauth2CredentialsRequest.Builder<B>> {

        protected Builder(TokenExchangeOauth2CredentialsRequest request) {
            super(request);
            grantType(GRANT_TYPE_TOKEN_EXCHANGE);
            param(SUBJECT_TOKEN_TYPE, TOKEN_TYPE_JWT);
            param(REQUESTED_TOKEN_TYPE, TOKEN_TYPE_JWT);
        }

        @JsonCreator
        public static <B extends TokenExchangeOauth2CredentialsRequest.Builder<B>> TokenExchangeOauth2CredentialsRequest.Builder<B> newInstance() {
            return new TokenExchangeOauth2CredentialsRequest.Builder<>(new TokenExchangeOauth2CredentialsRequest());
        }

        /**
         * The workload credential proving the identity of the process performing the exchange.
         */
        public B subjectToken(String subjectToken) {
            param(SUBJECT_TOKEN, subjectToken);
            return self();
        }

        /**
         * The type of the subject token, defaults to {@code urn:ietf:params:oauth:token-type:jwt}.
         */
        public B subjectTokenType(String subjectTokenType) {
            param(SUBJECT_TOKEN_TYPE, subjectTokenType);
            return self();
        }

        /**
         * The token type the broker is asked to mint, defaults to {@code urn:ietf:params:oauth:token-type:jwt}.
         */
        public B requestedTokenType(String requestedTokenType) {
            param(REQUESTED_TOKEN_TYPE, requestedTokenType);
            return self();
        }

        /**
         * The identifier of the receiving party that will consume the exchanged token.
         */
        public B audience(String audience) {
            param(AUDIENCE, audience);
            return self();
        }

        @Override
        public B self() {
            return (B) this;
        }

        @Override
        public TokenExchangeOauth2CredentialsRequest build() {
            Objects.requireNonNull(request.params.get(SUBJECT_TOKEN), SUBJECT_TOKEN);
            Objects.requireNonNull(request.params.get(SUBJECT_TOKEN_TYPE), SUBJECT_TOKEN_TYPE);
            Objects.requireNonNull(request.getResource(), "resource");
            Objects.requireNonNull(request.params.get(AUDIENCE), AUDIENCE);
            return super.build();
        }
    }
}

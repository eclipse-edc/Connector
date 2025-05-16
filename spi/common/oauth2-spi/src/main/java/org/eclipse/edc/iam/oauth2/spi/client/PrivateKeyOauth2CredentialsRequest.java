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

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public class PrivateKeyOauth2CredentialsRequest extends Oauth2CredentialsRequest {

    private static final String CLIENT_ASSERTION = "client_assertion";
    private static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    private static final String TYPE_JWT_BEARER = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    public Object getClientAssertion() {
        return params.get(CLIENT_ASSERTION);
    }

    public Object getClientAssertionType() {
        return params.get(CLIENT_ASSERTION_TYPE);
    }

    public static class Builder<B extends PrivateKeyOauth2CredentialsRequest.Builder<B>> extends Oauth2CredentialsRequest.Builder<PrivateKeyOauth2CredentialsRequest, PrivateKeyOauth2CredentialsRequest.Builder<B>> {

        protected Builder(PrivateKeyOauth2CredentialsRequest request) {
            super(request);
        }

        @JsonCreator
        public static <B extends PrivateKeyOauth2CredentialsRequest.Builder<B>> PrivateKeyOauth2CredentialsRequest.Builder<B> newInstance() {
            return new PrivateKeyOauth2CredentialsRequest.Builder<>(new PrivateKeyOauth2CredentialsRequest());
        }

        public B clientAssertion(String assertion) {
            param(CLIENT_ASSERTION, assertion);
            param(CLIENT_ASSERTION_TYPE, TYPE_JWT_BEARER);
            return self();
        }

        @Override
        public B self() {
            return (B) this;
        }

        @Override
        public PrivateKeyOauth2CredentialsRequest build() {
            Objects.requireNonNull(request.params.get(CLIENT_ASSERTION), CLIENT_ASSERTION);
            return super.build();
        }
    }
}

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

public class SharedSecretOauth2CredentialsRequest extends Oauth2CredentialsRequest {

    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";

    public Object getClientId() {
        return params.get(CLIENT_ID);
    }

    public Object getClientSecret() {
        return params.get(CLIENT_SECRET);
    }

    public static class Builder<B extends SharedSecretOauth2CredentialsRequest.Builder<B>> extends Oauth2CredentialsRequest.Builder<SharedSecretOauth2CredentialsRequest, SharedSecretOauth2CredentialsRequest.Builder<B>> {

        protected Builder(SharedSecretOauth2CredentialsRequest request) {
            super(request);
        }

        @JsonCreator
        public static <B extends SharedSecretOauth2CredentialsRequest.Builder<B>> SharedSecretOauth2CredentialsRequest.Builder<B> newInstance() {
            return new SharedSecretOauth2CredentialsRequest.Builder<>(new SharedSecretOauth2CredentialsRequest());
        }

        public B clientSecret(String secret) {
            param(CLIENT_SECRET, secret);
            return self();
        }

        public B clientId(String id) {
            param(CLIENT_ID, id);
            return self();
        }

        @Override
        public B self() {
            return (B) this;
        }

        @Override
        public SharedSecretOauth2CredentialsRequest build() {
            Objects.requireNonNull(request.params.get(CLIENT_ID), CLIENT_ID);
            Objects.requireNonNull(request.params.get(CLIENT_SECRET), CLIENT_SECRET);
            return super.build();
        }
    }
}

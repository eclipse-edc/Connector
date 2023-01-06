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

class TestRequest extends Oauth2CredentialsRequest {
    public static class Builder<B extends Builder<B>> extends Oauth2CredentialsRequest.Builder<TestRequest, Builder<B>> {

        protected Builder(TestRequest request) {
            super(request);
        }

        @JsonCreator
        public static <B extends Builder<B>> Builder<B> newInstance() {
            return new Builder<>(new TestRequest());
        }

        @Override
        public B self() {
            return (B) this;
        }

        @Override
        public TestRequest build() {
            return super.build();
        }
    }
}

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

package org.eclipse.edc.iam.identitytrust.sts.spi.model;

import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService;

import java.util.Objects;

/**
 * The {@link StsAccountTokenAdditionalParams} contains additional parameters for the {@link StsClientTokenGeneratorService }
 * when creating the Self-Issued ID token for the {@link StsAccount}
 */
public class StsAccountTokenAdditionalParams {

    private String bearerAccessScope;

    private String audience;
    private String accessToken;

    private StsAccountTokenAdditionalParams() {

    }

    public String getBearerAccessScope() {
        return bearerAccessScope;
    }

    public String getAudience() {
        return audience;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public static class Builder {

        private final StsAccountTokenAdditionalParams params;

        private Builder(StsAccountTokenAdditionalParams params) {
            this.params = params;
        }

        public static Builder newInstance() {
            return new Builder(new StsAccountTokenAdditionalParams());
        }


        public Builder audience(String audience) {
            params.audience = audience;
            return this;
        }

        public Builder bearerAccessScope(String bearerAccessScope) {
            params.bearerAccessScope = bearerAccessScope;
            return this;
        }

        public Builder accessToken(String accessToken) {
            params.accessToken = accessToken;
            return this;
        }

        public StsAccountTokenAdditionalParams build() {
            Objects.requireNonNull(params.audience, "Param audience missing");
            return params;
        }

    }
}

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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth2 Client Credential <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.4.3">Access Token Response</a>
 *
 * @param accessToken Self-Issued ID token.
 * @param expiresIn   Duration of the token.
 * @param tokenType   Token type.
 */
public record StsTokenResponse(@JsonProperty("access_token") String accessToken,
                               @JsonProperty("expires_in") Long expiresIn,
                               @JsonProperty("token_type") String tokenType) {

    public StsTokenResponse(String accessToken, Long expiresIn) {
        this(accessToken, expiresIn, "Bearer");
    }

}

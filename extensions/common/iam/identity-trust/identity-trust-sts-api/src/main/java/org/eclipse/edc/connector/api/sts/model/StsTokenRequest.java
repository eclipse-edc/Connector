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
 * @param grantType         Type of grant. Must be client_credentials.
 * @param clientId          Client ID identifier.
 * @param clientSecret      Authorization secret for the client.
 * @param bearerAccessScope Space-delimited scopes to be included in the access_token claim.
 * @param accessToken       VP/VC Access Token to be included as access_token claim.
 */
public record StsTokenRequest(@FormParam("grant_type") String grantType,
                              @FormParam("client_id") String clientId,
                              @FormParam("client_secret") String clientSecret,
                              @FormParam("bearer_access_scope") String bearerAccessScope,
                              @FormParam("access_token") String accessToken) {
}

/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.iam;

/**
 * Obtains client security tokens from an identity provider.
 * Providers may implement different authorization protocols such as OAuth2.
 */
public interface IdentityService {

    String FEATURE = "edc:iam";

    /**
     * Obtains a client token encoded as a JWT.
     */
    TokenResult obtainClientCredentials(String scope);

    /**
     * Verifies a JWT bearer token.
     *
     * @param token    the token to verify
     * @param audience the audience the token must be for
     */
    VerificationResult verifyJwtToken(String token, String audience);

}

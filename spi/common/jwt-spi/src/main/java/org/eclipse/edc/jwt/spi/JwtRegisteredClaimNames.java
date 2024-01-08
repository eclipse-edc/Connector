/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.jwt.spi;

/**
 * JSON Web Token Claim Names
 * ref. <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4.1">rfc7519 section-4.1</a>
 */
public final class JwtRegisteredClaimNames {

    /**
     * "client_id" (Client Identifier) Claim
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc8693#name-client_id-client-identifier">RFC 8693 "client_id" (Client Identifier) Claim</a>
     */

    public static final String CLIENT_ID = "client_id";

    /**
     * "scope" (Scopes) Claim
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc8693#name-scope-scopes-claim">RFC 8693 "scope" (Scopes) Claim</a>
     */
    public static final String SCOPE = "scope";

    private JwtRegisteredClaimNames() {
    }
}

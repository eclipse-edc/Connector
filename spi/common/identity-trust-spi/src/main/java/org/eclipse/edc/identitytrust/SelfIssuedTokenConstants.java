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

package org.eclipse.edc.identitytrust;

/**
 * Constants for the Self-Issued ID Token
 */
public final class SelfIssuedTokenConstants {

    /**
     * VP access token claim
     */
    public static final String ACCESS_TOKEN = "access_token";

    /**
     * Alias to be used in the sub of the VP access token
     */
    public static final String BEARER_ACCESS_ALIAS = "bearer_access_alias";

    /**
     * Scopes to be encoded in the VP access token
     */
    public static final String BEARER_ACCESS_SCOPE = "bearer_access_scope";
    
    private SelfIssuedTokenConstants() {

    }
}

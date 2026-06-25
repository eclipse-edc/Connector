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

package org.eclipse.edc.api.authentication;

public interface OauthTokenProvider {

    /**
     * Creates a token whose {@code sub} claim is the given participant context id (the principal identity).
     */
    String createToken(String participantContextId);

    /**
     * Creates an elevated, cross-tenant token carrying the {@code management-api:admin} scope.
     */
    String createAdminToken();
}

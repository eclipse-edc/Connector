/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.api.auth.spi;

import java.security.Principal;

/**
 * Represents the security principal of an authenticated Management API request. The {@code participantContextId} is the
 * identity carried by the token's {@code sub} claim; {@code scope} is the (space-delimited) set of granted scopes that
 * determine what the principal may do.
 */
public record ParticipantPrincipal(String participantContextId, String scope) implements Principal {

    @Override
    public String getName() {
        return participantContextId;
    }
}

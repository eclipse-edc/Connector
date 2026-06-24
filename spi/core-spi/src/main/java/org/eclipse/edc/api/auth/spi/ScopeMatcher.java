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

package org.eclipse.edc.api.auth.spi;

import org.eclipse.edc.spi.result.AbstractResult;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Evaluates whether a set of granted scopes (the space-delimited {@code scope} claim of an access token) satisfies a
 * required scope. The grammar and matching rules are defined by {@link Scope}.
 */
public class ScopeMatcher {

    private static final String SCOPE_SEPARATOR = " ";

    /**
     * Whether any of the {@code grantedScopes} (a space-delimited scope claim) satisfies the {@code requiredScope}.
     * A malformed {@code requiredScope} or unparseable granted entries never grant access.
     */
    public boolean isSatisfiedBy(String requiredScope, String grantedScopes) {
        var required = Scope.parse(requiredScope);
        if (required.failed()) {
            return false;
        }
        return parse(grantedScopes).stream().anyMatch(granted -> granted.satisfies(required.getContent()));
    }

    /**
     * Whether the {@code grantedScopes} convey full cross-tenant elevation, i.e. satisfy {@link ManagementApiScopes#ADMIN}
     * ({@code management-api:*:admin}).
     */
    public boolean isAdmin(String grantedScopes) {
        return isSatisfiedBy(ManagementApiScopes.ADMIN, grantedScopes);
    }

    private List<Scope> parse(String grantedScopes) {
        if (grantedScopes == null || grantedScopes.isBlank()) {
            return emptyList();
        }
        return Arrays.stream(grantedScopes.trim().split(SCOPE_SEPARATOR))
                .filter(s -> !s.isBlank())
                .map(Scope::parse)
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .toList();
    }
}

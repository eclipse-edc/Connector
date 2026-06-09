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

import org.eclipse.edc.api.auth.spi.Scope.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeTest {

    @Test
    void parse_twoSegment_defaultsResourceToWildcard() {
        var result = Scope.parse("management-api:read");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(new Scope("management-api", Scope.WILDCARD, Action.READ));
    }

    @Test
    void parse_threeSegment_keepsResource() {
        var result = Scope.parse("management-api:policies:write");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(new Scope("management-api", "policies", Action.WRITE));
    }

    @ParameterizedTest
    @ValueSource(strings = { "management-api:ADMIN", "management-api:Admin", "management-api:assets:WRITE" })
    void parse_actionIsCaseInsensitive(String raw) {
        assertThat(Scope.parse(raw).succeeded()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "management-api",                 // single segment
            "management-api:assets:foo:read", // too many segments
            "management-api:read:",           // blank trailing segment
            "management-api::read",           // blank resource
            "management-api:unknown",         // unknown action
            "management-api:assets:list"      // unknown action (3-segment)
    })
    void parse_invalid(String raw) {
        assertThat(Scope.parse(raw).failed()).isTrue();
    }

    @ParameterizedTest
    // grantedResource, grantedAction, requiredResource, requiredAction, expected
    @CsvSource({
            // exact resource, action hierarchy admin ⊇ write ⊇ read
            "assets, READ,  assets, READ,  true",
            "assets, WRITE, assets, READ,  true",
            "assets, ADMIN, assets, READ,  true",
            "assets, READ,  assets, WRITE, false",
            "assets, WRITE, assets, WRITE, true",
            "assets, ADMIN, assets, WRITE, true",
            "assets, WRITE, assets, ADMIN, false",
            "assets, ADMIN, assets, ADMIN, true",
            // wildcard granted resource covers any required resource
            "*,      WRITE, assets, WRITE, true",
            "*,      READ,  policies, READ, true",
            // specific granted resource does NOT cover a different (or wildcard) required resource
            "assets, WRITE, policies, WRITE, false",
            "assets, READ,  *,      READ,  false"
    })
    void satisfies(String grantedResource, Action grantedAction, String requiredResource, Action requiredAction, boolean expected) {
        var granted = new Scope("management-api", grantedResource, grantedAction);
        var required = new Scope("management-api", requiredResource, requiredAction);

        assertThat(granted.satisfies(required)).isEqualTo(expected);
    }

    @Test
    void satisfies_differentPrefix_isFalse() {
        var granted = new Scope("other-api", "*", Action.ADMIN);
        var required = new Scope("management-api", "assets", Action.READ);

        assertThat(granted.satisfies(required)).isFalse();
    }
}

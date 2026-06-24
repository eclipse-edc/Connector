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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeMatcherTest {

    private final ScopeMatcher matcher = new ScopeMatcher();

    @ParameterizedTest
    // requiredScope, grantedScopes, expected
    @CsvSource({
            // required read is satisfied by read, write or admin
            "management-api:read,  management-api:read,  true",
            "management-api:read,  management-api:write, true",
            "management-api:read,  management-api:admin, true",
            // required write is satisfied by write or admin, not by read
            "management-api:write, management-api:read,  false",
            "management-api:write, management-api:write, true",
            "management-api:write, management-api:admin, true",
            // required admin only by admin
            "management-api:admin, management-api:write, false",
            "management-api:admin, management-api:admin, true",
            // wildcard (coarse) granted scope satisfies a specific (fine-grained) required scope
            "management-api:policies:write, management-api:write, true",
            "management-api:policies:read,  management-api:admin, true",
            // a specific granted scope does not satisfy a different resource
            "management-api:assets:write, management-api:policies:write, false"
    })
    void isSatisfiedBy(String requiredScope, String grantedScopes, boolean expected) {
        assertThat(matcher.isSatisfiedBy(requiredScope, grantedScopes)).isEqualTo(expected);
    }

    @Test
    void isSatisfiedBy_picksMatchingScopeFromMultiValuedClaim() {
        var granted = "openid profile management-api:assets:read management-api:policies:write";

        assertThat(matcher.isSatisfiedBy("management-api:policies:write", granted)).isTrue();
        assertThat(matcher.isSatisfiedBy("management-api:assets:read", granted)).isTrue();
        assertThat(matcher.isSatisfiedBy("management-api:assets:write", granted)).isFalse();
    }

    @Test
    void isSatisfiedBy_ignoresUnparseableGrantedEntries() {
        var granted = "not-a-scope another:bad:one:here management-api:write";

        assertThat(matcher.isSatisfiedBy("management-api:read", granted)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "openid profile" })
    void isSatisfiedBy_noMatchingGrantedScope_isFalse(String granted) {
        assertThat(matcher.isSatisfiedBy("management-api:read", granted)).isFalse();
    }

    @Test
    void isSatisfiedBy_malformedRequiredScope_isFalse() {
        assertThat(matcher.isSatisfiedBy("not-a-valid-scope", "management-api:admin")).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "management-api:admin,                 true",
            "openid management-api:admin,          true",
            "management-api:write,                 false",
            "management-api:read,                  false",
            // resource-scoped admin is NOT full cross-tenant elevation
            "management-api:participants:admin,    false"
    })
    void isAdmin(String grantedScopes, boolean expected) {
        assertThat(matcher.isAdmin(grantedScopes)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isAdmin_blank_isFalse(String granted) {
        assertThat(matcher.isAdmin(granted)).isFalse();
    }
}

/*
 *  Copyright (c) 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.core.base.policy;

import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBindingRegistryImplTest {
    private RuleBindingRegistryImpl registry;

    @Test
    void verifyScopeBinding() {
        registry.bind("rule1", "scope1");

        assertThat(registry.isInScope("rule1", "scope1")).isTrue();
        assertThat(registry.isInScope("rule1", "scope2")).isFalse();
    }

    @Test
    void verifyWildcardScopeBinding() {
        registry.bind("rule1", PolicyEngine.ALL_SCOPES);

        assertThat(registry.isInScope("rule1", "scope1")).isTrue();
    }

    @Test
    void verifyScopeBindingAreInheritedByChildScopes() {
        registry.bind("rule1", "scope1");

        assertThat(registry.isInScope("rule1", "scope1.child")).isTrue();
    }

    @Test
    void verifyChildScopeBindingsAreNotVisibleInParent() {
        registry.bind("rule1", "scope1.child");

        assertThat(registry.isInScope("rule1", "scope1")).isFalse();
    }

    @BeforeEach
    void setUp() {
        registry = new RuleBindingRegistryImpl();
    }
}

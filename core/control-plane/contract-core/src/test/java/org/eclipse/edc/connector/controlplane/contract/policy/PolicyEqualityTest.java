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

package org.eclipse.edc.connector.controlplane.contract.policy;

import org.eclipse.edc.connector.controlplane.contract.policy.PolicyEquality;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.policy.model.PolicyType.CONTRACT;
import static org.eclipse.edc.policy.model.PolicyType.OFFER;

class PolicyEqualityTest {

    private final PolicyEquality comparator = new PolicyEquality(new TypeManager());

    @Test
    void emptyPoliciesAreEqual() {
        var one = Policy.Builder.newInstance().build();
        var two = Policy.Builder.newInstance().build();

        var result = comparator.test(one, two);

        assertThat(result).isTrue();
    }

    @Test
    void ifDifferentRulesPoliciesAreNotEqual() {
        var one = Policy.Builder.newInstance().permission(Permission.Builder.newInstance().build()).build();
        var two = Policy.Builder.newInstance().build();

        var result = comparator.test(one, two);

        assertThat(result).isFalse();
    }

    @Test
    void policyTypeIsExcludedFromTheComparison() {
        var one = Policy.Builder.newInstance().type(OFFER).build();
        var two = Policy.Builder.newInstance().type(CONTRACT).build();

        var result = comparator.test(one, two);

        assertThat(result).isTrue();
    }

    @Test
    void assigneeIsExcludedFromTheComparison() {
        var one = Policy.Builder.newInstance().assignee("one").build();
        var two = Policy.Builder.newInstance().assignee("other").build();

        var result = comparator.test(one, two);

        assertThat(result).isTrue();
    }
}

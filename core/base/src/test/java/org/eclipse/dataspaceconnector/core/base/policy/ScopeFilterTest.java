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

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AndConstraint;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.policy.RuleBindingRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeFilterTest {
    public static final String BOUND_SCOPE = "scope1";
    private static final Action REPORT_ACTION = Action.Builder.newInstance().type("report").build();
    private static final Action SUB_ACTION = Action.Builder.newInstance().type("subaction").build();
    private static final LiteralExpression BOUND_LITERAL = new LiteralExpression("bound");
    private static final LiteralExpression UNBOUND_LITERAL = new LiteralExpression("unbound");
    private static final LiteralExpression EMPTY_LITERAL = new LiteralExpression("");
    public static final AtomicConstraint BOUND_CONSTRAINT = AtomicConstraint.Builder.newInstance().leftExpression(BOUND_LITERAL).rightExpression(EMPTY_LITERAL).build();
    public static final AtomicConstraint UNBOUND_CONSTRAINT = AtomicConstraint.Builder.newInstance().leftExpression(UNBOUND_LITERAL).rightExpression(EMPTY_LITERAL).build();
    private ScopeFilter scopeFilter;
    private RuleBindingRegistry registry;

    @Test
    void verifyFiltersUnboundPermissionType() {
        var permission = Permission.Builder.newInstance().action(REPORT_ACTION).build();

        assertThat(scopeFilter.applyScope(permission, "unbound.scope")).isNull();
    }


    @Test
    void verifyFiltersPolicy() {
        registry.bind(REPORT_ACTION.getType(), BOUND_SCOPE);
        var permission = Permission.Builder.newInstance().action(REPORT_ACTION).build();
        var duty = Duty.Builder.newInstance().action(REPORT_ACTION).build();
        var prohibition = Prohibition.Builder.newInstance().action(REPORT_ACTION).build();

        var policy = Policy.Builder.newInstance()
                .assignee("assignee")
                .assigner("assigner")
                .target("target")
                .inheritsFrom("test")
                .type(PolicyType.CONTRACT)
                .permission(permission)
                .duty(duty)
                .prohibition(prohibition)
                .extensibleProperty("key", "value")
                .build();

        var filteredPolicy = scopeFilter.applyScope(policy, BOUND_SCOPE);

        assertThat(filteredPolicy).isNotNull();
        assertThat(filteredPolicy.getAssignee()).isNotNull();
        assertThat(filteredPolicy.getAssigner()).isNotNull();
        assertThat(filteredPolicy.getTarget()).isNotNull();
        assertThat(filteredPolicy.getInheritsFrom()).isNotNull();
        assertThat(filteredPolicy.getType()).isNotNull();
        assertThat(filteredPolicy.getPermissions()).isNotEmpty();
        assertThat(filteredPolicy.getObligations()).isNotEmpty();
        assertThat(filteredPolicy.getProhibitions()).isNotEmpty();
        assertThat(filteredPolicy.getExtensibleProperties()).isNotEmpty();

    }

    @Test
    void verifyFiltersPermissionType() {
        registry.bind(REPORT_ACTION.getType(), BOUND_SCOPE);
        registry.bind(SUB_ACTION.getType(), BOUND_SCOPE);
        registry.bind(BOUND_LITERAL.asString(), BOUND_SCOPE);

        var childDuty = Duty.Builder.newInstance()
                .action(SUB_ACTION)
                .build();
        var permission = Permission.Builder.newInstance()
                .action(REPORT_ACTION)
                .uid("1")
                .assignee("assignee")
                .assigner("assigner")
                .target("target")
                .constraint(BOUND_CONSTRAINT)
                .constraint(UNBOUND_CONSTRAINT)
                .duty(childDuty)
                .build();

        var filteredPermission = scopeFilter.applyScope(permission, BOUND_SCOPE);

        assertThat(filteredPermission).isNotNull();
        assertThat(filteredPermission.getAction()).isNotNull();
        assertThat(filteredPermission.getAssignee()).isNotNull();
        assertThat(filteredPermission.getAssigner()).isNotNull();
        assertThat(filteredPermission.getTarget()).isNotNull();
        assertThat(filteredPermission.getUid()).isNotNull();
        assertThat(filteredPermission.getDuties()).isNotEmpty();
        assertThat(filteredPermission.getConstraints().size()).isEqualTo(1);  // verify that the unbound constraint was removed
        assertThat(filteredPermission.getConstraints()).contains(BOUND_CONSTRAINT);
    }

    @Test
    void verifyFiltersUnboundProhibitionType() {
        var prohibition = Prohibition.Builder.newInstance().action(REPORT_ACTION).build();

        assertThat(scopeFilter.applyScope(prohibition, "unbound.scope")).isNull();
    }

    @Test
    void verifyFiltersProhibitionType() {
        registry.bind(REPORT_ACTION.getType(), BOUND_SCOPE);
        registry.bind(SUB_ACTION.getType(), BOUND_SCOPE);
        registry.bind(BOUND_LITERAL.asString(), BOUND_SCOPE);

        var prohibition = Prohibition.Builder.newInstance()
                .action(REPORT_ACTION)
                .uid("1")
                .assignee("assignee")
                .assigner("assigner")
                .target("target")
                .constraint(BOUND_CONSTRAINT)
                .constraint(UNBOUND_CONSTRAINT)
                .build();

        var filteredPermission = scopeFilter.applyScope(prohibition, BOUND_SCOPE);

        assertThat(filteredPermission).isNotNull();
        assertThat(filteredPermission.getAction()).isNotNull();
        assertThat(filteredPermission.getAssignee()).isNotNull();
        assertThat(filteredPermission.getAssigner()).isNotNull();
        assertThat(filteredPermission.getTarget()).isNotNull();
        assertThat(filteredPermission.getUid()).isNotNull();
        assertThat(filteredPermission.getConstraints().size()).isEqualTo(1);  // verify that the unbound constraint was removed
        assertThat(filteredPermission.getConstraints()).contains(BOUND_CONSTRAINT);
    }

    @Test
    void verifyFiltersUnboundDutyType() {
        var duty = Duty.Builder.newInstance().action(REPORT_ACTION).build();

        assertThat(scopeFilter.applyScope(duty, "unbound.scope")).isNull();
    }

    @Test
    void verifyFiltersDutyType() {
        registry.bind(REPORT_ACTION.getType(), BOUND_SCOPE);
        registry.bind(SUB_ACTION.getType(), BOUND_SCOPE);
        registry.bind(BOUND_LITERAL.asString(), BOUND_SCOPE);

        var consequence = Duty.Builder.newInstance()
                .action(SUB_ACTION)
                .build();

        var duty = Duty.Builder.newInstance()
                .action(REPORT_ACTION)
                .uid("1")
                .assignee("assignee")
                .assigner("assigner")
                .target("target")
                .constraint(BOUND_CONSTRAINT)
                .constraint(UNBOUND_CONSTRAINT)
                .consequence(consequence)
                .build();

        var filteredDuty = scopeFilter.applyScope(duty, BOUND_SCOPE);

        assertThat(filteredDuty).isNotNull();
        assertThat(filteredDuty.getAction()).isNotNull();
        assertThat(filteredDuty.getAssignee()).isNotNull();
        assertThat(filteredDuty.getAssigner()).isNotNull();
        assertThat(filteredDuty.getTarget()).isNotNull();
        assertThat(filteredDuty.getUid()).isNotNull();
        assertThat(filteredDuty.getConsequence()).isNotNull();
        assertThat(filteredDuty.getConstraints().size()).isEqualTo(1);  // verify that the unbound constraint was removed
        assertThat(filteredDuty.getConstraints()).contains(BOUND_CONSTRAINT);
    }

    @Test
    void verifyMultiplicityConstraint() {
        registry.bind(BOUND_LITERAL.asString(), BOUND_SCOPE);

        var constraint = AndConstraint.Builder.newInstance()
                .constraint(BOUND_CONSTRAINT)
                .constraint(BOUND_CONSTRAINT)
                .build();

        var filteredConstraint = (MultiplicityConstraint) scopeFilter.applyScope(constraint, BOUND_SCOPE);

        assertThat(filteredConstraint).isNotNull();
        assertThat(filteredConstraint.getConstraints().size()).isEqualTo(2);
    }

    @BeforeEach
    void setUp() {
        registry = new RuleBindingRegistryImpl();
        scopeFilter = new ScopeFilter(registry);
    }
}

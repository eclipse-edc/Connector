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

import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.policy.model.XoneConstraint;
import org.eclipse.dataspaceconnector.spi.policy.RuleBindingRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Filters a policy for a scope. This involves recursively removing rules and constraints not bound to the scope and returning a modified copy of the unfiltered policy.
 *
 * The following filtering rules are implemented:
 *
 * <ul>
 *     <li>If root rule (i.e. directly contained in the {@link Policy} is processed and it rule action is not bound to a scope, the rule will be removed.</li>
 *     <li>If a root rule's action is bound but the action of a contained child rule is not, the root rule will be included but the child will be removed.</li>
 *     <li>If an {@link AtomicConstraint}'s left-operand </li> is not bound, it will be removed from the containing {@link Rule} or {@link MultiplicityConstraint}.
 * </ul>
 *
 * It is possible that all constraints in a rule are filtered. In this case, the rule will always evaluate to true. Filtering also has the potential to alter the semantics of
 * {@link MultiplicityConstraint}s if only some contained constraints are filtered. For example, removing constraints from {@link XoneConstraint} may evaluate to false if
 * child constraints are removed. Care should therefore be taken when defining rule bindings.
 */
public class ScopeFilter {
    private RuleBindingRegistry registry;

    public ScopeFilter(RuleBindingRegistry registry) {
        this.registry = registry;
    }

    public Policy applyScope(Policy policy, String scope) {
        var filteredObligations = policy.getObligations().stream().map(d -> applyScope(d, scope)).filter(Objects::nonNull).collect(toList());
        var filteredPermissions = policy.getPermissions().stream().map(p -> applyScope(p, scope)).filter(Objects::nonNull).collect(toList());
        var filteredProhibitions = policy.getProhibitions().stream().map(p -> applyScope(p, scope)).filter(Objects::nonNull).collect(toList());
        var policyBuilder = Policy.Builder.newInstance();
        policyBuilder.id(policy.getUid())
                .type(policy.getType())
                .assignee(policy.getAssignee())
                .assigner(policy.getAssigner())
                .inheritsFrom(policy.getInheritsFrom())
                .target(policy.getTarget())
                .duties(filteredObligations)
                .permissions(filteredPermissions)
                .prohibitions(filteredProhibitions)
                .extensibleProperties(policy.getExtensibleProperties());
        return policyBuilder.build();
    }

    @Nullable
    Permission applyScope(Permission permission, String scope) {
        if (actionNotInScope(permission, scope)) {
            return null;
        }
        var filteredConstraints = applyScope(permission.getConstraints(), scope);
        var filteredDuties = permission.getDuties().stream().map(d -> applyScope(d, scope)).filter(Objects::nonNull).collect(toList());

        return Permission.Builder.newInstance()
                .uid(permission.getUid())
                .action(permission.getAction())
                .assignee(permission.getAssignee())
                .assigner(permission.getAssigner())
                .target(permission.getTarget())
                .constraints(filteredConstraints)
                .duties(filteredDuties)
                .build();
    }

    @Nullable
    Duty applyScope(Duty duty, String scope) {
        if (actionNotInScope(duty, scope)) {
            return null;
        }
        var filteredConsequence = duty.getConsequence() != null ? applyScope(duty.getConsequence(), scope) : null;
        var filteredConstraints = applyScope(duty.getConstraints(), scope);

        return Duty.Builder.newInstance()
                .uid(duty.getUid())
                .action(duty.getAction())
                .assignee(duty.getAssignee())
                .assigner(duty.getAssigner())
                .target(duty.getTarget())
                .constraints(filteredConstraints)
                .parentPermission(duty.getParentPermission())
                .consequence(filteredConsequence)
                .build();
    }

    @Nullable
    Prohibition applyScope(Prohibition prohibition, String scope) {
        if (actionNotInScope(prohibition, scope)) {
            return null;
        }
        var filteredConstraints = applyScope(prohibition.getConstraints(), scope);

        return Prohibition.Builder.newInstance()
                .uid(prohibition.getUid())
                .action(prohibition.getAction())
                .assignee(prohibition.getAssignee())
                .assigner(prohibition.getAssigner())
                .target(prohibition.getTarget())
                .constraints(filteredConstraints)
                .build();
    }

    @Nullable
    Constraint applyScope(Constraint rootConstraint, String scope) {
        if (rootConstraint instanceof AtomicConstraint) {
            return applyScope((AtomicConstraint) rootConstraint, scope);
        } else if (rootConstraint instanceof MultiplicityConstraint) {
            var multiplicityConstraint = (MultiplicityConstraint) rootConstraint;
            var filteredConstraints = multiplicityConstraint.getConstraints().stream().map(c -> applyScope(c, scope)).filter(Objects::nonNull).collect(toList());
            return filteredConstraints.isEmpty() ? null : multiplicityConstraint.create(filteredConstraints);
        }
        return rootConstraint;
    }

    private boolean actionNotInScope(Rule rule, String scope) {
        return rule.getAction() != null && !registry.isInScope(rule.getAction().getType(), scope);
    }

    private List<Constraint> applyScope(List<Constraint> constraints, String scope) {
        return constraints.stream().map(constraint -> applyScope(constraint, scope)).filter(Objects::nonNull).collect(toList());
    }

    @Nullable
    private Constraint applyScope(AtomicConstraint constraint, String scope) {
        if (constraint.getLeftExpression() instanceof LiteralExpression) {
            return registry.isInScope(((LiteralExpression) constraint.getLeftExpression()).asString(), scope) ? constraint : null;
        } else {
            return constraint;
        }
    }
}

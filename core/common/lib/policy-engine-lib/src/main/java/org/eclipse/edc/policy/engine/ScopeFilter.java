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

package org.eclipse.edc.policy.engine;

import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Filters a policy for a scope. This involves recursively removing rules and constraints not bound to the scope and
 * returning a modified copy of the unfiltered policy.
 * <p>
 * The following filtering rules are implemented:
 *
 * <ul>
 *     <li>If root rule (i.e. directly contained in the {@link Policy} is processed and it rule action is not bound to a scope, the rule will be removed.</li>
 *     <li>If a root rule's action is bound but the action of a contained child rule is not, the root rule will be included but the child will be removed.</li>
 *     <li>If an {@link AtomicConstraint}'s left-operand is not bound, it will be removed from the containing {@link Rule} or {@link MultiplicityConstraint}.</li>
 * </ul>
 * <p>
 * It is possible that all constraints in a rule are filtered. In this case, the rule will always evaluate to true. Filtering also has the potential to alter the semantics of
 * {@link MultiplicityConstraint}s if only some contained constraints are filtered. For example, removing constraints from {@link XoneConstraint} may evaluate to false if
 * child constraints are removed. Care should therefore be taken when defining rule bindings.
 */
public class ScopeFilter {
    private final RuleBindingRegistry registry;

    public ScopeFilter(RuleBindingRegistry registry) {
        this.registry = registry;
    }

    public Policy applyScope(Policy policy, String scope) {
        var filteredObligations = filterBy(policy.getObligations(), d -> applyScope(d, scope));
        var filteredPermissions = filterBy(policy.getPermissions(), d -> applyScope(d, scope));
        var filteredProhibitions = filterBy(policy.getProhibitions(), d -> applyScope(d, scope));
        return Policy.Builder.newInstance()
                .type(policy.getType())
                .assignee(policy.getAssignee())
                .assigner(policy.getAssigner())
                .inheritsFrom(policy.getInheritsFrom())
                .target(policy.getTarget())
                .duties(filteredObligations)
                .permissions(filteredPermissions)
                .prohibitions(filteredProhibitions)
                .extensibleProperties(policy.getExtensibleProperties())
                .build();
    }

    @Nullable
    Permission applyScope(Permission permission, String scope) {
        if (actionNotInScope(permission, scope)) {
            return null;
        }
        var filteredConstraints = filterBy(permission.getConstraints(), c -> applyScope(c, scope));
        var filteredDuties = filterBy(permission.getDuties(), d -> applyScope(d, scope));

        return Permission.Builder.newInstance()
                .action(permission.getAction())
                .constraints(filteredConstraints)
                .duties(filteredDuties)
                .build();
    }

    @Nullable
    Duty applyScope(Duty duty, String scope) {
        if (actionNotInScope(duty, scope)) {
            return null;
        }
        var filteredConsequences = filterBy(duty.getConsequences(), d -> applyScope(d, scope));
        var filteredConstraints = filterBy(duty.getConstraints(), c -> applyScope(c, scope));

        return Duty.Builder.newInstance()
                .action(duty.getAction())
                .constraints(filteredConstraints)
                .consequences(filteredConsequences)
                .build();
    }

    @Nullable
    Prohibition applyScope(Prohibition prohibition, String scope) {
        if (actionNotInScope(prohibition, scope)) {
            return null;
        }
        var filteredConstraints = filterBy(prohibition.getConstraints(), c -> applyScope(c, scope));
        var filteredRemedies = filterBy(prohibition.getRemedies(), d -> applyScope(d, scope));

        return Prohibition.Builder.newInstance()
                .action(prohibition.getAction())
                .constraints(filteredConstraints)
                .remedies(filteredRemedies)
                .build();
    }

    @Nullable
    Constraint applyScope(Constraint rootConstraint, String scope) {
        if (rootConstraint instanceof AtomicConstraint atomicConstraint) {
            return applyScope(atomicConstraint, scope);
        } else if (rootConstraint instanceof MultiplicityConstraint multiplicityConstraint) {
            var filteredConstraints = multiplicityConstraint.getConstraints().stream().map(c -> applyScope(c, scope)).filter(Objects::nonNull).toList();
            return filteredConstraints.isEmpty() ? null : multiplicityConstraint.create(filteredConstraints);
        }
        return rootConstraint;
    }

    private boolean actionNotInScope(Rule rule, String scope) {
        return rule.getAction() != null && !registry.isInScope(rule.getAction().getType(), scope);
    }

    @Nullable
    private Constraint applyScope(AtomicConstraint constraint, String scope) {
        if (constraint.getLeftExpression() instanceof LiteralExpression literalExpression) {
            return registry.isInScope(literalExpression.asString(), scope) ? constraint : null;
        } else {
            return constraint;
        }
    }

    private <T> List<T> filterBy(List<T> list, Function<T, T> filterFunction) {
        return list.stream().map(filterFunction).filter(Objects::nonNull).toList();
    }
}

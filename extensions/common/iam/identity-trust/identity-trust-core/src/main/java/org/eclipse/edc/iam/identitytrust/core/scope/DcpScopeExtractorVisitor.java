/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.core.scope;

import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractor;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.XoneConstraint;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DCP scope visitor for invoking {@link ScopeExtractor}s during the pre-validation phase.
 */
public class DcpScopeExtractorVisitor implements Policy.Visitor<Set<String>>, Rule.Visitor<Set<String>>, Constraint.Visitor<Set<String>>, Expression.Visitor<Object> {

    private final List<ScopeExtractor> mappers;
    private final RequestPolicyContext policyContext;

    public DcpScopeExtractorVisitor(List<ScopeExtractor> mappers, RequestPolicyContext policyContext) {
        this.mappers = mappers;
        this.policyContext = policyContext;
    }

    @Override
    public Set<String> visitAndConstraint(AndConstraint andConstraint) {
        return visitMultiplicityConstraint(andConstraint);
    }

    @Override
    public Set<String> visitOrConstraint(OrConstraint orConstraint) {
        return visitMultiplicityConstraint(orConstraint);
    }

    @Override
    public Set<String> visitXoneConstraint(XoneConstraint constraint) {
        return visitMultiplicityConstraint(constraint);
    }

    @Override
    public Set<String> visitAtomicConstraint(AtomicConstraint constraint) {
        var rightValue = constraint.getRightExpression().accept(this);
        var leftRawValue = constraint.getLeftExpression().accept(this);

        return mappers.stream()
                .map(mapper -> mapper.extractScopes(leftRawValue, constraint.getOperator(), rightValue, policyContext))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

    }

    @Override
    public Object visitLiteralExpression(LiteralExpression expression) {
        return expression.getValue();
    }

    @Override
    public Set<String> visitPolicy(Policy policy) {
        var scopes = new HashSet<String>();
        policy.getPermissions().forEach(permission -> scopes.addAll(permission.accept(this)));
        policy.getProhibitions().forEach(prohibition -> scopes.addAll(prohibition.accept(this)));
        policy.getObligations().forEach(duty -> scopes.addAll(duty.accept(this)));
        return scopes;
    }

    @Override
    public Set<String> visitPermission(Permission policy) {
        var scopes = policy.getDuties().stream()
                .map(duty -> duty.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        scopes.addAll(visitRule(policy));
        return scopes;
    }

    @Override
    public Set<String> visitProhibition(Prohibition policy) {
        return visitRule(policy);
    }

    @Override
    public Set<String> visitDuty(Duty policy) {
        return visitRule(policy);
    }

    private Set<String> visitRule(Rule rule) {
        return rule.getConstraints().stream()
                .map(constraint -> constraint.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Set<String> visitMultiplicityConstraint(MultiplicityConstraint multiplicityConstraint) {
        return multiplicityConstraint.getConstraints().stream()
                .map(constraint -> constraint.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}

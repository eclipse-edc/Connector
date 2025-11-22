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

package org.eclipse.edc.iam.decentralizedclaims.core.scope;

import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.XoneConstraint;

import java.util.List;

public class ScopeTestFunctions {

    public static Policy permissionPolicy(Constraint constraint) {
        var permission = Permission.Builder.newInstance()
                .constraint(constraint)
                .build();

        return Policy.Builder.newInstance()
                .permission(permission)
                .build();
    }

    public static AtomicConstraint atomicConstraint(Object left, Operator operator, Object right) {
        return AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression(left))
                .operator(operator)
                .rightExpression(new LiteralExpression(right))
                .build();

    }

    public static AndConstraint andConstraint(List<Constraint> constraints) {
        return AndConstraint.Builder.newInstance()
                .constraints(constraints)
                .build();

    }

    public static OrConstraint orConstraint(List<Constraint> constraints) {
        return OrConstraint.Builder.newInstance()
                .constraints(constraints)
                .build();

    }

    public static XoneConstraint xoneConstraint(List<Constraint> constraints) {
        return XoneConstraint.Builder.newInstance()
                .constraints(constraints)
                .build();

    }

    public static Policy dutyPolicy(Constraint constraint) {
        var duty = Duty.Builder.newInstance()
                .constraint(constraint)
                .build();

        return Policy.Builder.newInstance()
                .duty(duty)
                .build();
    }

    public static Policy prohibitionPolicy(Constraint constraint) {
        var prohibition = Prohibition.Builder.newInstance()
                .constraint(constraint)
                .build();

        return Policy.Builder.newInstance()
                .prohibition(prohibition)
                .build();
    }
}

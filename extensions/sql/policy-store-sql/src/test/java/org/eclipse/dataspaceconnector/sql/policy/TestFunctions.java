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

package org.eclipse.dataspaceconnector.sql.policy;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;

import java.util.List;

public class TestFunctions {
    public static PolicyDefinition createPolicy(String id) {
        var policy = createPolicyBuilder(id).build();

        return PolicyDefinition.Builder.newInstance().uid(id).policy(policy).build();
    }

    public static Policy.Builder createPolicyBuilder(String id) {
        var permission = Permission.Builder.newInstance()
                .uid(id)
                .build();

        var prohibition = createProhibitionBuilder(id)
                .build();

        var duty = Duty.Builder.newInstance()
                .uid(id)
                .build();

        var p = Policy.Builder.newInstance()
                .permission(permission)
                .prohibition(prohibition)
                .duties(List.of(duty))
                .inheritsFrom("sampleInheritsFrom")
                .assigner("sampleAssigner")
                .assignee("sampleAssignee")
                .target("sampleTarget")
                .type(PolicyType.SET);
        return p;
    }

    public static Prohibition.Builder createProhibitionBuilder(String id) {
        return Prohibition.Builder.newInstance()
                .uid(id);
    }

    public static Duty.Builder createDutyBuilder(String id) {
        return Duty.Builder.newInstance()
                .uid(id);
    }

    public static Permission.Builder createPermissionBuilder(String id) {
        return Permission.Builder.newInstance().uid(id);
    }

    public static Action createAction() {
        return Action.Builder.newInstance().constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression("foo"))
                        .operator(Operator.EQ)
                        .rightExpression(new LiteralExpression("bar"))
                        .build())
                .type("test-action-type")
                .build();
    }

    public static QuerySpec createQuery(String filterExpression) {
        return QuerySpec.Builder.newInstance().filter(filterExpression).build();
    }
}

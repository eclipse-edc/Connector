/*
 *  Copyright (c) 2021 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.ids.policy;

import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.policy.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;

import java.util.Collection;
import java.util.Objects;

public class AbsSpatialPositionConstraintFunction implements AtomicConstraintFunction<Permission> {

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        var region = context.getParticipantAgent().getClaims().get("region");
        switch (operator) {
            case EQ:
                return Objects.equals(region, rightValue);
            case NEQ:
                return !Objects.equals(region, rightValue);
            case IN:
                return ((Collection<?>) rightValue).contains(region);
            default:
                return false;
        }
    }

}

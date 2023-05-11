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

package org.eclipse.edc.connector.contract.validation;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class ContractExpiryCheckFunction implements AtomicConstraintFunction<Permission> {

    public static final String CONTRACT_EXPIRY_EVALUATION_KEY = EDC_NAMESPACE + "inForceDate";

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        try {
            var now = getContextData(Instant.class, context);
            var time = Instant.parse(rightValue.toString());
            var comparison = now.compareTo(time);

            switch (operator) {
                case EQ:
                    return comparison == 0;
                case NEQ:
                    return comparison != 0;
                case GT:
                    return comparison > 0;
                case GEQ:
                    return comparison >= 0;
                case LT:
                    return comparison < 0;
                case LEQ:
                    return comparison <= 0;
                case IN:
                default:
                    throw new IllegalStateException("Unexpected value: " + operator);
            }

        } catch (NullPointerException | DateTimeParseException ex) {
            context.reportProblem(ex.getMessage());
            return false;
        }
    }

    private <R> R getContextData(Class<R> clazz, PolicyContext context) {
        return Objects.requireNonNull(context.getContextData(clazz), clazz.getSimpleName());
    }

}

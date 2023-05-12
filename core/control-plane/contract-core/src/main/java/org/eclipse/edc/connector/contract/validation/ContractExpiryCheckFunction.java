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

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.EdcException;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Constraint function that evaluates a time-based constraint. That is a constraint that either uses the "inForceDate" operand
 * with a fixed time (ISO-8061 UTC) as:
 * <pre>
 * {
 *   "leftOperand": "edc:inForceDate",
 *   "operator": "GEQ",
 *   "rightOperand": "2024-01-01T00:00:01Z"
 * }
 * </pre>
 * Alternatively, it is possible to use a duration expression:
 * <pre>
 * {
 *   "leftOperand": "edc:inForceDate",
 *   "operator": "GEQ",
 *   "rightOperand": "contractAgreement+365d"
 * }
 * </pre>
 * following the following schema: {@code <offset> + <numeric value>s|m|h|d} where {@code offset} must be equal to {@code "contractAgreement"}
 * (not case-sensitive) and refers to the signing date of the contract in Epoch seconds. Omitting the {@code offset} is not permitted.
 * The numeric value can have negative values.
 * Thus, the following examples would be valid:
 * <ul>
 *     <li>contractAgreement+15s</li>
 *     <li>contractAgreement+7d</li>
 *     <li>contractAgreement+1h</li>
 *     <li>contractAgreement+-5m (means "5 minutes before the signing of the contract")</li>
 * </ul>
 * Please note that all {@link Operator}s except {@link Operator#IN} are supported.
 */
public class ContractExpiryCheckFunction implements AtomicConstraintFunction<Permission> {

    public static final String CONTRACT_EXPIRY_EVALUATION_KEY = EDC_NAMESPACE + "inForceDate";
    private static final String EXPRESSION_REGEX = "(contract[A,a]greement)\\+(-?[0-9]+)(s|m|h|d)";
    private static final int REGEX_GROUP_NUMERIC = 2;
    private static final int REGEX_GROUP_UNIT = 3;


    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        if (!(rightValue instanceof String)) {
            context.reportProblem("Right-value expected to be String but was " + rightValue.getClass());
            return false;
        }

        try {
            var now = getContextData(Instant.class, context);

            var rightValueStr = (String) rightValue;
            var bound = asInstant(rightValueStr);
            if (bound != null) {
                return checkFixedPeriod(now, operator, bound);
            }

            var duration = asDuration(rightValueStr);
            if (duration != null) {
                var agreement = getContextData(ContractAgreement.class, context);
                var signingDate = Instant.ofEpochSecond(agreement.getContractSigningDate());
                return checkFixedPeriod(now, operator, signingDate.plus(duration));
            }

            context.reportProblem(format("Unsupported right-value, expected either an ISO-8061 String or a expression matching '%s', but got '%s'",
                    CONTRACT_EXPIRY_EVALUATION_KEY, rightValueStr));

        } catch (NullPointerException | DateTimeParseException ex) {
            context.reportProblem(ex.getMessage());
        }
        return false;
    }

    /**
     * Checks whether an input string fits the regex {@link ContractExpiryCheckFunction#EXPRESSION_REGEX}, e.g. "contractAgreement+50m"
     * and parses that string into a {@link Duration} if successful.
     *
     * @param rightValueStr A string potentially containing a duration expression.
     * @return A {@link Duration} or null if input doesn't match
     */
    private Duration asDuration(String rightValueStr) {
        var matcher = Pattern.compile(EXPRESSION_REGEX).matcher(rightValueStr);
        if (matcher.matches()) {
            var number = Integer.parseInt(matcher.group(REGEX_GROUP_NUMERIC));
            var unit = matcher.group(REGEX_GROUP_UNIT);
            return Duration.of(number, asChrono(unit));
        } else {
            return null;
        }
    }

    /**
     * parses a string ["s","h","d"] into the respective chrono unit
     *
     * @return the {@link TemporalUnit}
     * @throws EdcException if the string was not recognized
     */
    private TemporalUnit asChrono(String unit) {
        switch (unit) {
            case "s":
                return ChronoUnit.SECONDS;
            case "m":
                return ChronoUnit.MINUTES;
            case "h":
                return ChronoUnit.HOURS;
            case "d":
                return ChronoUnit.DAYS;
            default:
                throw new EdcException(format("Cannot parse '%s' into a ChronoUnit", unit));
        }
    }

    private boolean checkFixedPeriod(Instant now, Operator operator, Instant bound) {
        var comparison = now.compareTo(bound);

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
    }

    private Instant asInstant(String isoString) {
        try {
            return Instant.parse(isoString);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private <R> R getContextData(Class<R> clazz, PolicyContext context) {
        return Objects.requireNonNull(context.getContextData(clazz), clazz.getSimpleName());
    }

}

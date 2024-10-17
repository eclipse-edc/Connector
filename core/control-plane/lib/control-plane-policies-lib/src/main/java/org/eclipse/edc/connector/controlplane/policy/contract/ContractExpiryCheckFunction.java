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

package org.eclipse.edc.connector.controlplane.policy.contract;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.EdcException;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

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
public class ContractExpiryCheckFunction<C extends AgreementPolicyContext> implements AtomicConstraintRuleFunction<Permission, C> {

    public static final String CONTRACT_EXPIRY_EVALUATION_KEY = EDC_NAMESPACE + "inForceDate";

    private static final String EXPRESSION_REGEX = "(contract[A,a]greement)\\+(-?[0-9]+)(s|m|h|d)";
    private static final int REGEX_GROUP_NUMERIC = 2;
    private static final int REGEX_GROUP_UNIT = 3;

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, C context) {
        if (rightValue == null) {
            context.reportProblem("Right-value is null.");
            return false;
        }

        if (!(rightValue instanceof String rightValueStr)) {
            context.reportProblem("Right-value expected to be String but was " + rightValue.getClass());
            return false;
        }

        return Optional.ofNullable(asInstant(rightValueStr))
                .or(() -> Optional.ofNullable(asDuration(rightValueStr))
                        .map(duration -> Instant.ofEpochSecond(context.contractAgreement().getContractSigningDate())
                                .plus(duration)
                        )
                ).map(bound -> checkFixedPeriod(context.now(), operator, bound))
                .orElseGet(() -> {
                    var message = "Unsupported right-value, expected either an ISO-8061 String or a expression matching '%s', but got '%s'"
                            .formatted(ContractExpiryCheckFunction.CONTRACT_EXPIRY_EVALUATION_KEY, rightValueStr);
                    context.reportProblem(message);
                    return false;
                });
    }

    /**
     * Checks whether an input string fits the regex {@link #EXPRESSION_REGEX}, e.g. "contractAgreement+50m"
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
        return switch (unit) {
            case "s" -> ChronoUnit.SECONDS;
            case "m" -> ChronoUnit.MINUTES;
            case "h" -> ChronoUnit.HOURS;
            case "d" -> ChronoUnit.DAYS;
            default -> throw new EdcException(format("Cannot parse '%s' into a ChronoUnit", unit));
        };
    }

    private boolean checkFixedPeriod(Instant now, Operator operator, Instant bound) {
        var comparison = now.compareTo(bound);

        return switch (operator) {
            case EQ -> comparison == 0;
            case NEQ -> comparison != 0;
            case GT -> comparison > 0;
            case GEQ -> comparison >= 0;
            case LT -> comparison < 0;
            case LEQ -> comparison <= 0;
            default -> throw new IllegalStateException("Unexpected value: " + operator);
        };
    }

    private Instant asInstant(String isoString) {
        try {
            return Instant.parse(isoString);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

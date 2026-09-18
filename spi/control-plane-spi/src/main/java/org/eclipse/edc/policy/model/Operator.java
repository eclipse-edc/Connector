/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_SCHEMA;

/**
 * The set of supported ODRL expression operators. Not all operators may be supported for particular expression types.
 */
public enum Operator {

    /**
     * Indicating that a given value equals the right operand of the Constraint.
     */
    EQ("eq"),

    /**
     * Indicating that a given value is not equal to the right operand of the Constraint.
     */
    NEQ("neq"),

    /**
     * Indicating that a given value is greater than the right operand of the Constraint.
     */
    GT("gt"),

    /**
     * Indicating that a given value is greater than or equal to the right operand of the Constraint.
     */
    GEQ("gteq"),

    /**
     * Indicating that a given value is less than the right operand of the Constraint.
     */
    LT("lt"),

    /**
     * Indicating that a given value is less than or equal to the right operand of the Constraint.
     */
    LEQ("lteq"),

    /**
     * A set-based operator indicating that a given value is contained by the right operand of the Constraint.
     */
    IS_PART_OF("isPartOf"),

    /**
     * A set-based operator indicating that a given value is contained by the right operand of the Constraint.
     *
     * @deprecated use {@link #IS_PART_OF} instead.
     */
    @Deprecated(since = "1.0.0")
    IN("isPartOf"),

    /**
     * A set-based operator indicating that a given value contains the right operand of the Constraint.
     */
    HAS_PART("hasPart"),

    /**
     * A set-based operator indicating that a given value is contained by the right operand of the Constraint.
     */
    IS_A("isA"),

    /**
     * A set-based operator indicating that a given value is all of the right operand of the Constraint.
     */
    IS_ALL_OF("isAllOf"),

    /**
     * A set-based operator indicating that a given value is any of the right operand of the Constraint.
     */
    IS_ANY_OF("isAnyOf"),

    /**
     * A set-based operator indicating that a given value is none of the right operand of the Constraint.
     */
    IS_NONE_OF("isNoneOf");

    private final String odrlRepresentation;

    Operator(String odrlRepresentation) {
        this.odrlRepresentation = ODRL_SCHEMA + odrlRepresentation;
    }

    public String getOdrlRepresentation() {
        return odrlRepresentation;
    }

    /**
     * Handles deserialization of deprecated IN value into IS_PART_OF.
     * This method should not be deleted for some time also after the deletion of the IN value to prevent unexpected errors
     *
     * @param value the value.
     * @return the operator.
     */
    @JsonCreator
    public static Operator fromString(String value) {
        if (value.equals("IN")) {
            return IS_PART_OF;
        }

        return Operator.valueOf(value);
    }

    /**
     * Handles serialization of deprecated IN value into IS_PART_OF.
     * This method should not be deleted for some time also after the deletion of the IN value to prevent unexpected errors
     *
     * @return serialized value.
     */
    @JsonValue
    public String toJson() {
        if (name().equals("IN")) {
            return IS_PART_OF.name();
        }
        return name();
    }

}

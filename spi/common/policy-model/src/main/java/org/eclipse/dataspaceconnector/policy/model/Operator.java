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

package org.eclipse.dataspaceconnector.policy.model;

/**
 * The set of supported expression operators. Not all operators may be supported for particular expression types.
 */
public enum Operator {
    /**
     * Operator expressing equality of two operands
     */
    EQ,
    /**
     * Operator expressing inequality of two operands
     */
    NEQ,
    /**
     * Operator expressing left operand is greater than right operand
     */
    GT,
    /**
     * Operator expressing left operand is greater or equal than to the right operand
     */
    GEQ,
    /**
     * Operator expressing left operand is lesser than to the right operand
     */
    LT,
    /**
     * Operator expressing left operand is lesser or equal than to the right operand
     */
    LEQ,
    /**
     * Operator expressing left operand is contained in the right operand
     */
    IN
}

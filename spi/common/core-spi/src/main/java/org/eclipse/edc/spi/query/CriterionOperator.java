/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.spi.query;

/**
 * Represent an operator that can be used in a Criterion
 *
 * @param representation the string representation.
 * @param rightOperandClass the expected right operand class.
 * @param predicate the predicate.
 */
public record CriterionOperator(String representation, Class<?> rightOperandClass, OperatorPredicate predicate) {
}

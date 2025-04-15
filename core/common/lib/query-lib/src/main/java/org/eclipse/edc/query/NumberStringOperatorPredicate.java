/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.query;

import org.eclipse.edc.spi.query.OperatorPredicate;

import java.util.Comparator;
import java.util.function.Predicate;

/**
 * Compares two {@link Number}s or two {@link String} based on the comparison predicate that evaluates the "direction"
 * of the comparison, if the result is 0, the two parameters are equal, if &lt; 0 the first is less than the second,
 * if &gt; 0 the first is greater than the second.
 */
public class NumberStringOperatorPredicate implements OperatorPredicate {

    private final Predicate<Integer> comparisonPredicate;

    public static OperatorPredicate equal() {
        return new NumberStringOperatorPredicate(it -> it == 0);
    }

    public static OperatorPredicate lessThan() {
        return new NumberStringOperatorPredicate(it -> it < 0);
    }

    public static OperatorPredicate lessThanEqual() {
        return new NumberStringOperatorPredicate(it -> it <= 0);
    }

    public static OperatorPredicate greaterThan() {
        return new NumberStringOperatorPredicate(it -> it > 0);
    }

    public static OperatorPredicate greaterThanEqual() {
        return new NumberStringOperatorPredicate(it -> it >= 0);
    }

    public NumberStringOperatorPredicate(Predicate<Integer> comparisonPredicate) {
        this.comparisonPredicate = comparisonPredicate;
    }

    @Override
    public boolean test(Object value, Object comparedTo) {
        if (value instanceof Number number1 && comparedTo instanceof Number number2) {
            return comparisonPredicate.test(Double.compare(number1.doubleValue(), number2.doubleValue()));
        }

        if (value instanceof String string1 && comparedTo instanceof String string2) {
            return comparisonPredicate.test(Comparator.<String>naturalOrder().compare(string1, string2));
        }

        return false;
    }
}

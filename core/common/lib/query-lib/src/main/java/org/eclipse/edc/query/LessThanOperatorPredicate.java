/*
 *  Copyright (c) 2024 Cofinity-X
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

public class LessThanOperatorPredicate implements OperatorPredicate {
    @Override
    public boolean test(Object value, Object comparedTo) {
        if (value instanceof Number number1 && comparedTo instanceof Number number2) {
            return Double.compare(number1.doubleValue(), number2.doubleValue()) < 0;
        }

        if (value instanceof String string1 && comparedTo instanceof String string2) {
            return Comparator.<String>naturalOrder().compare(string1, string2) < 0;
        }

        return false;
    }
}

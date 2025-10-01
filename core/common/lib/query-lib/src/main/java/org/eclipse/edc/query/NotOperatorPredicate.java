/*
 *  Copyright (c) 2025 Think-it GmbH
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

package org.eclipse.edc.query;

import org.eclipse.edc.spi.query.OperatorPredicate;

/**
 * Decorator that negates the passed operator
 */
public class NotOperatorPredicate implements OperatorPredicate {

    private final OperatorPredicate delegate;

    public static OperatorPredicate not(OperatorPredicate delegate) {
        return new NotOperatorPredicate(delegate);
    }

    public NotOperatorPredicate(OperatorPredicate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean test(Object o, Object o2) {
        return delegate.negate().test(o, o2);
    }
}

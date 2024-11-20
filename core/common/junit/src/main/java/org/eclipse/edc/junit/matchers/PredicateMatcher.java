/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.junit.matchers;

import org.mockito.ArgumentMatcher;

import java.util.function.Predicate;

@Deprecated(since = "0.11.0") // as not used anywhere
public class PredicateMatcher<T> implements ArgumentMatcher<T> {

    private final Predicate<T> predicate;

    public PredicateMatcher(Predicate<T> predicate) {

        this.predicate = predicate;
    }

    @Override
    public boolean matches(T argument) {
        return predicate.test(argument);
    }
}

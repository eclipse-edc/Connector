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

package org.eclipse.edc.junit.matchers;

import org.mockito.internal.matchers.Equals;

import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.argThat;

/**
 * Matches if the items contained in the "wanted" array are contained in the actual one.
 * Heavily inspired on {@link org.mockito.internal.matchers.ArrayEquals}
 */
public class ArrayContainsMatcher extends Equals {

    @SuppressWarnings("unchecked")
    public static <T> T[] arrayContains(T[] items) {
        return (T[]) argThat(new ArrayContainsMatcher(items));
    }

    public ArrayContainsMatcher(Object[] wanted) {
        super(wanted);
    }

    @Override
    public boolean matches(Object actual) {
        var wanted = getWanted();
        if (wanted instanceof Object[] wantedArray && actual instanceof Object[] actualArray) {
            return new HashSet<>(Arrays.asList(actualArray)).containsAll(Arrays.asList(wantedArray));
        }

        return super.matches(actual);
    }
}

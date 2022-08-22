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

package org.eclipse.dataspaceconnector.boot.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TopologicalSortTest {

    @Test
    void verifySort() {
        var sort = new TopologicalSort<String>();
        sort.addDependency("foo", "bar");
        sort.addDependency("bar", "baz");

        List<String> list = new ArrayList<>();
        list.add("baz");
        list.add("bar");
        list.add("foo");
        list.add("quux");

        sort.sort(list);

        assertEquals("baz", list.get(0));
        assertEquals("bar", list.get(1));
        assertEquals("foo", list.get(2));
    }

    @Test
    void verifyCyclicDependency() {
        var sort = new TopologicalSort<String>();
        sort.addDependency("foo", "bar");
        sort.addDependency("bar", "foo");

        List<String> list = new ArrayList<>();
        list.add("baz");
        list.add("bar");
        list.add("foo");

        assertThrows(CyclicDependencyException.class, () -> sort.sort(list));
    }
}

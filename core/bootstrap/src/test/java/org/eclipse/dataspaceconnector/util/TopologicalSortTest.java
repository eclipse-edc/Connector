/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.util;

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

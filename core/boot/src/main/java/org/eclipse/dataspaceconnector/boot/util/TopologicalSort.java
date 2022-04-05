/*
 *  Copyright (c) 2021 Microsoft Corporation
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

//
//  Code based on TopologicalSort from Eclipse Jetty licensed under Apache 2.0 (https://www.eclipse.org/jetty/).
//
//  Original license notice:
//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Topologically sorts a set of dependencies.
 */
public class TopologicalSort<T> {
    private final Map<T, Set<T>> dependencies = new HashMap<>();

    /**
     * Add a dependency to be considered in the sort.
     *
     * @param dependent  The dependent item will be sorted after all its dependencies
     * @param dependency The dependency item, will be sorted before its dependent item
     */
    public void addDependency(T dependent, T dependency) {
        Set<T> set = dependencies.computeIfAbsent(dependent, k -> new HashSet<>());
        set.add(dependency);
    }

    /**
     * Sort the passed list according to dependencies previously set with
     * {@link #addDependency(Object, Object)}. Where possible, ordering will be
     * preserved if no dependency
     *
     * @param list The list to be sorted.
     */
    public void sort(Collection<T> list) {
        List<T> sorted = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        Comparator<T> comparator = new InitialOrderComparator<>(list);

        for (T t : list) {
            visit(t, visited, sorted, comparator);
        }

        list.clear();
        list.addAll(sorted);
    }

    /**
     * Visit an item to be sorted.
     *
     * @param item       the item to be visited
     * @param visited    the items already visited
     * @param sorted     the sorted items
     * @param comparator comparator used to sort dependencies
     */
    private void visit(T item, Set<T> visited, List<T> sorted, Comparator<T> comparator) {
        if (!visited.contains(item)) {
            visited.add(item);

            // get the item's dependencies
            Set<T> dependencies = this.dependencies.get(item);
            if (dependencies != null) {
                // sort the dependencies
                SortedSet<T> orderedDependencies = new TreeSet<>(comparator);
                orderedDependencies.addAll(dependencies);

                // recursively visit each dependency
                try {
                    for (T d : orderedDependencies) {
                        visit(d, visited, sorted, comparator);
                    }
                } catch (CyclicDependencyException e) {
                    throw new CyclicDependencyException(item, e);
                }
            }

            // the transitive set of dependencies has been visited and added to the sorted list; add the current item after the dependencies
            sorted.add(item);
        } else if (!sorted.contains(item)) {
            // if an item has been visited but is not in the sorted list, it is a cycle
            throw new CyclicDependencyException(item);
        }
    }


    /**
     * Sorts dependencies in the order they were in the original list. This ensures that dependencies are visited in the original order and no needless reordering is performed.
     */
    private static class InitialOrderComparator<T> implements Comparator<T> {
        private final Map<T, Integer> indexes = new HashMap<>();

        InitialOrderComparator(Collection<T> initial) {
            int i = 0;
            for (T t : initial) {
                indexes.put(t, i++);
            }
        }

        @Override
        public int compare(T o1, T o2) {
            Integer i1 = indexes.get(o1);
            Integer i2 = indexes.get(o2);
            if (i1 == null || i2 == null || i1.equals(o2)) {
                return 0;
            }
            return i1 < i2 ? -1 : 1;
        }
    }

}

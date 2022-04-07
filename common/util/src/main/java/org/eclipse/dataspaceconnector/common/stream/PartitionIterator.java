/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.common.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

/**
 * Partitions a source collection into units.
 */
public class PartitionIterator<T> implements Iterator<List<T>> {
    
    private final Iterator<T> source;
    private final int partitionSize;

    public static <T> Stream<List<T>> streamOf(Stream<T> source, int partitionSize) {
        var iterator = new PartitionIterator<>(source.iterator(), partitionSize);
        return stream(spliteratorUnknownSize(iterator, ORDERED), false);
    }

    public PartitionIterator(Iterator<T> source, int partitionSize) {
        this.source = Objects.requireNonNull(source);

        if (partitionSize <= 0) {
            throw new IllegalArgumentException("Invalid partition size:" + partitionSize);
        }
        this.partitionSize = partitionSize;
    }

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    @Override
    public List<T> next() {
        List<T> partition = new ArrayList<>(partitionSize);
        while (source.hasNext() && partition.size() < partitionSize) {
            partition.add(source.next());
        }
        return partition;
    }
}

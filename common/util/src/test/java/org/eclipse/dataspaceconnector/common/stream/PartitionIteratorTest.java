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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartitionIteratorTest {

    @Test
    void verifyStream() {
        var partitions = PartitionIterator.streamOf(Stream.of("1", "2", "3", "4"), 2).collect(toList());
        assertThat(partitions.size()).isEqualTo(2);
        assertThat(partitions.get(0)).contains("1", "2");
        assertThat(partitions.get(1)).contains("3", "4");
    }

    @Test
    void verifyEqualPartitions() {
        var iterator = new PartitionIterator<>(List.of("1", "2", "3", "4").iterator(), 2);
        var partitions = stream(spliteratorUnknownSize(iterator, ORDERED), false).collect(toList());
        assertThat(partitions.size()).isEqualTo(2);
        assertThat(partitions.get(0)).contains("1", "2");
        assertThat(partitions.get(1)).contains("3", "4");
    }

    @Test
    void verifyUnequalPartitions() {
        var iterator = new PartitionIterator<>(List.of("1", "2", "3", "4").iterator(), 3);
        var partitions = stream(spliteratorUnknownSize(iterator, ORDERED), false).collect(toList());
        assertThat(partitions.size()).isEqualTo(2);
        assertThat(partitions.get(0)).contains("1", "2", "3");
        assertThat(partitions.get(1)).contains("4");
    }

    @Test
    void verifyOnePartition() {
        var iterator = new PartitionIterator<>(List.of("1", "2", "3", "4").iterator(), 4);
        var partitions = stream(spliteratorUnknownSize(iterator, ORDERED), false).collect(toList());
        assertThat(partitions.size()).isEqualTo(1);
        assertThat(partitions.get(0)).contains("1", "2", "3", "4");
    }

    @Test
    void verifyGreaterThanCollectionSize() {
        var iterator = new PartitionIterator<>(List.of("1", "2", "3", "4").iterator(), 5);
        var partitions = stream(spliteratorUnknownSize(iterator, ORDERED), false).collect(toList());
        assertThat(partitions.size()).isEqualTo(1);
        assertThat(partitions.get(0)).contains("1", "2", "3", "4");
    }

    @Test
    void verifyInvalidPartitionSize() {
        assertThatThrownBy(() -> new PartitionIterator<>(List.of("1", "2", "3", "4").iterator(), 0));
    }


}

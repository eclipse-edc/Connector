/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 */
package org.eclipse.dataspaceconnector.spi.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedCommandQueueTest {

    private static final int QUEUE_BOUND = 3;
    private BoundedTestCommandQueue queue;

    @BeforeEach
    void setup() {
        queue = new BoundedTestCommandQueue(QUEUE_BOUND);
    }

    @Test
    void enqueue_withCapacityToSpare() {
        var cmd = new TestCommand();
        queue.enqueue(cmd);

        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    void enqueue_queueFull_shouldReturnExceptionalFuture() {
        IntStream.range(0, 3).forEach(i -> queue.enqueue(new TestCommand()));
        assertThatThrownBy(() -> queue.enqueue(new TestCommand())).isInstanceOf(IllegalStateException.class);
    }


    @Test
    void dequeueSingle_onEmptyQueue_shouldNotBlock() {
        assertThat(queue.dequeue()).isNull();
    }

    @Test
    void dequeueSingle_hasElement() {
        var element = new TestCommand();
        queue.enqueue(element);

        assertThat(queue.dequeue()).isEqualTo(element);
        assertThat(queue.size()).isEqualTo(0);

    }

    @Test
    void dequeueMultiple_onEmptyQueue() {
        assertThat(queue.dequeue(5)).isNotNull().isEmpty();
    }

    @Test
    void dequeueMultiple_withNegativeAmount() {
        assertThatThrownBy(() -> queue.dequeue(-5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dequeueMultiple_amountGreaterThanQueueSize() {
        IntStream.range(0, 2).forEach(i -> queue.enqueue(new TestCommand()));
        assertThat(queue.dequeue(5)).isNotEmpty().hasSize(2);
        assertThat(queue.size()).isEqualTo(0);
    }

    @Test
    void dequeueMultiple_amountEqualToQueueSize() {
        IntStream.range(0, 2).forEach(i -> queue.enqueue(new TestCommand()));
        assertThat(queue.dequeue(2)).isNotEmpty().hasSize(2);
        assertThat(queue.size()).isEqualTo(0);
    }

    @Test
    void dequeueMultiple_amountSmallerThanQueueSize() {
        var c1 = new TestCommand();
        var c2 = new TestCommand();
        var c3 = new TestCommand();
        queue.enqueue(c1);
        queue.enqueue(c2);
        queue.enqueue(c3);
        assertThat(queue.dequeue(2)).isNotEmpty().containsExactly(c1, c2);
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    void peek_onEmptyQueue() {
        assertThat(queue.peek()).isNull();
    }

    @Test
    void peek_onNonEmptyQueue() {
        var element = new TestCommand();
        queue.enqueue(element);

        assertThat(queue.peek()).isEqualTo(element);
        assertThat(queue.size()).isEqualTo(1);
    }

    private static class TestCommand extends Command {
    }

    private static class BoundedTestCommandQueue extends BoundedCommandQueue<TestCommand> {
        public BoundedTestCommandQueue(int bound) {
            super(bound);
        }
    }

}

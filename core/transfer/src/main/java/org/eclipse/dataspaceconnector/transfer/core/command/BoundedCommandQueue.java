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
 *
 */
package org.eclipse.dataspaceconnector.transfer.core.command;

import org.eclipse.dataspaceconnector.spi.command.Command;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * In-memory implementation of the {@link CommandQueue} that is backed by a bounded queue. This means that attempting
 * to add elements to an already full queue will fail and raise an {@link IllegalStateException}.
 * <p>
 * This queue is threadsafe, so every operation on it can be assumed atomic.
 */
public class BoundedCommandQueue implements CommandQueue {
    private final BlockingQueue<Command> queue;

    public BoundedCommandQueue(int bound) {
        queue = new ArrayBlockingQueue<>(bound);
    }

    @Override
    public void enqueue(Command element) {
        //add will throw an IllegalStateException if the queue exceeds its capacity
        queue.add(element);
    }

    @Nullable
    @Override
    public Command dequeue() {
        return queue.poll();
    }

    @Override
    public List<Command> dequeue(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException();
        }

        var result = new ArrayList<Command>();
        queue.drainTo(result, amount);
        return result;
    }

    @Nullable
    @Override
    public Command peek() {
        return queue.peek();
    }

    public int size() {
        return queue.size();
    }
}

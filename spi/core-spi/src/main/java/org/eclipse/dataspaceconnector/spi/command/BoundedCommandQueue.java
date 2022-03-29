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
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */

package org.eclipse.dataspaceconnector.spi.command;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BoundedCommandQueue<C extends Command> implements CommandQueue<C> {

    private final BlockingQueue<C> queue;

    public BoundedCommandQueue(int bound) {
        queue = new ArrayBlockingQueue<>(bound);
    }

    @Override
    public void enqueue(C element) {
        //add will throw an IllegalStateException if the queue exceeds its capacity
        queue.add(element);
    }

    @Nullable
    @Override
    public Command dequeue() {
        return queue.poll();
    }

    @Override
    public List<C> dequeue(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException();
        }

        var result = new ArrayList<C>();
        queue.drainTo(result, amount);
        return result;
    }

    @Nullable
    @Override
    public C peek() {
        return queue.peek();
    }

    public int size() {
        return queue.size();
    }

}

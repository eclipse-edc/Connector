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
 *
 */
package org.eclipse.dataspaceconnector.spi.command;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CommandQueue<C extends Command> {

    /**
     * Adds one element to the command queue. In case the queue cannot accept any element, e.g. because it is full, the returned {@link CompletableFuture} immediately completes exceptionally. If the command
     * was successfully enqueued, but the command could not be processed, the future will also complete exceptionally, potentially after retrying.
     * In any other cases the future completes as soon as the command was successfully processed.
     *
     * @param element The element to add
     * @see Command#getFuture()
     */
    void enqueue(C element);

    /**
     * Removes and returns a single element
     *
     * @return The first element in the queue. {@code null} if the queue is empty
     */
    @Nullable
    Command dequeue();

    /**
     * Removes and returns the specified number of elements from the head of the queue.
     *
     * @param amount The maximum number of items. If the queue holds fewer items than specified in {@code amount}, all items are returned.
     * @return A list of items, maybe empty, never null.
     * @throws IllegalArgumentException if {@code amount} is < zero.
     */
    List<C> dequeue(int amount);

    /**
     * Returns but does not remove the first element.
     *
     * @return The first element in the queue. {@code null} if the queue is empty
     */
    @Nullable
    C peek();
}

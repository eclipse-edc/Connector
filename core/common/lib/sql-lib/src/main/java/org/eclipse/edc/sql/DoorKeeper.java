/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql;

import org.eclipse.edc.spi.EdcException;

import java.util.Stack;

/**
 * The DoorKeeper takes care to close all the registered {@link AutoCloseable} components from the last to the first
 * Wrapping eventually checked exceptions with an unchecked {@link EdcException}
 */
class DoorKeeper implements AutoCloseable {
    private final Stack<AutoCloseable> closeables = new Stack<>();

    /**
     * Add a {@link AutoCloseable} component to be closed
     *
     * @param closable the component to be closed
     * @return itself
     */
    public DoorKeeper takeCareOf(AutoCloseable closable) {
        closeables.push(closable);
        return this;
    }

    /**
     * Iterate over the components to be closed and close them from the last to first.
     */
    @Override
    public void close() {
        while (!closeables.empty()) {
            var closeable = closeables.pop();
            try {
                closeable.close();
            } catch (Exception e) {
                throw new EdcException(e);
            }
        }
    }
}

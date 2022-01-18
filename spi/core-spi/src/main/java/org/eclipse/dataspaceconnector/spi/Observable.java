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
 *       Fraunhofer Institute for Software and Systems Engineering - extended method implementation
 *
 */

package org.eclipse.dataspaceconnector.spi;


import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public abstract class Observable<T> {

    private final Collection<T> listeners;

    protected Observable() {
        listeners = new ConcurrentLinkedQueue<>();
    }

    public Collection<T> getListeners() {
        return listeners;
    }

    public void registerListener(T listener) {

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(T listener) {
        listeners.remove(listener);
    }
    
    /**
     * Invokes a given action on all registered listeners.
     *
     * @param action the action to invoke.
     */
    protected void invokeForEach(Consumer<T> action) {
        getListeners().forEach(action);
    }
}

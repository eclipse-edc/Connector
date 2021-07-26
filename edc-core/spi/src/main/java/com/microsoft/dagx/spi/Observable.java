/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi;


import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    public void unregister(T listener) {
        listeners.remove(listener);
    }
}

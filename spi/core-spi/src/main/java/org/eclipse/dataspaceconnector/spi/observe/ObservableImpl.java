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
 *
 */

package org.eclipse.dataspaceconnector.spi.observe;


import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ObservableImpl<T> implements Observable<T> {

    private final Collection<T> listeners;

    public ObservableImpl() {
        listeners = new ConcurrentLinkedQueue<>();
    }

    @Override
    public Collection<T> getListeners() {
        return listeners;
    }

    @Override
    public void registerListener(T listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(T listener) {
        listeners.remove(listener);
    }
}

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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - extended method implementation
 *
 */

package org.eclipse.edc.spi.observe;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Tracks listeners and invokes callbacks for specified events.
 */
public interface Observable<T> {

    /**
     * Returns the registered listeners.
     */
    Collection<T> getListeners();

    /**
     * Registers a listener.
     */
    void registerListener(T listener);

    /**
     * Unregisters a listener.
     */
    void unregisterListener(T listener);

    /**
     * Invokes a given action on all registered listeners.
     *
     * @param action the action to invoke.
     */
    default void invokeForEach(Consumer<T> action) {
        getListeners().forEach(action);
    }
}

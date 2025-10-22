/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.junit.utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Lazy implementation of the {@link Supplier} interface.
 */
public class LazySupplier<T> implements Supplier<T> {

    private final Supplier<T> dataSupplier;
    private final AtomicReference<T> data = new AtomicReference<>();

    public LazySupplier(Supplier<T> dataSupplier) {
        this.dataSupplier = dataSupplier;
    }

    @Override
    public T get() {
        var currentValue = data.get();
        if (currentValue == null) {
            var newValue = dataSupplier.get();
            data.compareAndExchange(null, newValue);
            return newValue;
        }
        return currentValue;
    }

}

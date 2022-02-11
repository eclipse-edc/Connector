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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * An in-memory {@link Handler} registry.
 */
public class HandlerRegistry<T> {
    private final Map<Class<? extends T>, Handler> handlers = new HashMap<>();

    public void addHandler(Class<? extends T> clazz, Handler handler) {
        handlers.put(clazz, handler);
    }

    public @Nullable Handler getHandler(Class<? extends T> clazz) {
        return Arrays.stream(clazz.getInterfaces())
                .map(handlers::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

    }
}

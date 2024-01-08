/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.token;

import org.eclipse.edc.jwt.spi.JwtDecorator;
import org.eclipse.edc.jwt.spi.JwtDecoratorRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class JwtDecoratorRegistryImpl implements JwtDecoratorRegistry {

    private final List<JwtDecorator> list = new CopyOnWriteArrayList<>();

    @Override
    public void register(JwtDecorator decorator) {
        list.add(decorator);
    }

    @Override
    public void unregister(JwtDecorator decorator) {
        list.remove(decorator);
    }

    @Override
    public Collection<JwtDecorator> getAll() {
        return new ArrayList<>(list);
    }
}

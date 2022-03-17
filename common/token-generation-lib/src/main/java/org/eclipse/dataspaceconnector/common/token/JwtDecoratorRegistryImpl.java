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

package org.eclipse.dataspaceconnector.common.token;

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

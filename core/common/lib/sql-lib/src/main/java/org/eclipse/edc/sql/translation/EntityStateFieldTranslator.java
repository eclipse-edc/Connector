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

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.entity.StateResolver;
import org.eclipse.edc.spi.query.Criterion;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

/**
 * Supports the string representation of a state, that will be converted into int by the {@link #stateResolver}.
 */
public class EntityStateFieldTranslator extends PlainColumnFieldTranslator {

    private final StateResolver stateResolver;

    public EntityStateFieldTranslator(String columnName, StateResolver stateResolver) {
        super(columnName);
        this.stateResolver = stateResolver;
    }

    @Override
    public Collection<Object> toParameters(Criterion criterion) {
        return super.toParameters(criterion)
                .stream()
                .map(it -> it instanceof String stringState ? stateResolver.resolve(stringState) : it)
                .collect(toList());
    }
}

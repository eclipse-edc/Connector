/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.defaults.storage;

import org.eclipse.edc.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.edc.util.reflection.ReflectionUtil;

/**
 * Default concrete implementation of {@link BaseCriterionToPredicateConverter}
 *
 * @param <T> the object type.
 */
public class DefaultCriterionToPredicateConverter<T> extends BaseCriterionToPredicateConverter<T> {
    @Override
    protected Object property(String key, Object object) {
        return ReflectionUtil.getFieldValueSilent(key, object);
    }
}

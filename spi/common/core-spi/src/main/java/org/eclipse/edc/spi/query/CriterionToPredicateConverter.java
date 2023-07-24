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

package org.eclipse.edc.spi.query;

import java.util.function.Predicate;

/**
 * Converts a {@link Criterion} into a {@link Predicate} that a specific store implementation
 * requires in order to perform queries against its backend.
 */
@FunctionalInterface
public interface CriterionToPredicateConverter {
    /**
     * converts a {@link Criterion} into an AssetIndex-specific query object.
     *
     * @param <T> The type of object which the store requires to perform its query.
     * @throws IllegalArgumentException if the criterion cannot be converted.
     */
    <T> Predicate<T> convert(Criterion criterion);
}

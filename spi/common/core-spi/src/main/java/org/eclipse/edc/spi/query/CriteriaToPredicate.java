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

package org.eclipse.edc.spi.query;

import java.util.List;
import java.util.function.Predicate;

/**
 * Converts a criterion list to a {@link Predicate}.
 *
 * @param <T> the predicate enclosed type.
 */
public interface CriteriaToPredicate<T> {

    /**
     * Convert a criteria into a predicate
     *
     * @param criteria the criteria.
     * @return the predicate.
     */
    Predicate<T> convert(List<Criterion> criteria);

}

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

package org.eclipse.dataspaceconnector.spi.query;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;

/**
 * Converts a {@link Criterion} into whatever statement a specific {@link AssetIndex} implementation
 * requires in order to perform queries against its backend.
 * For example, an AssetIndex backed by SQL an SQL-conformant query string, thus a {@code CriterionConverter}
 * must convert the Criterion into a String.
 *
 * @param <T> The type of object which the {@link AssetIndex} requires to perform its query.
 */
@FunctionalInterface
public interface CriterionConverter<T> {
    /**
     * converts a {@link Criterion} into an AssetIndex-specific query object.
     *
     * @throws IllegalArgumentException if the criterion cannot be converted.
     */
    T convert(Criterion criterion);
}

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

import java.util.stream.Stream;

/**
 * Interface responsible for querying a stream by provided {@link QuerySpec}.
 *
 * @param <T> type of the queried elements.
 */
@FunctionalInterface
public interface QueryResolver<T> {

    /**
     * Method to query a stream by provided specification.
     *
     * @param stream stream to be queried.
     * @param spec   query specification.
     * @return stream result from queries.
     */
    Stream<T> query(Stream<T> stream, QuerySpec spec);

}

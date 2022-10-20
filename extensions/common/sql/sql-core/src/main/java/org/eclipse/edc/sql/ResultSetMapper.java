/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.edc.sql;

import java.sql.ResultSet;

/**
 * Component capable of mapping {@link ResultSet} to e.g. POJO.
 *
 * @param <T> generic type param of the resulting type
 */
@FunctionalInterface
public interface ResultSetMapper<T> {

    /**
     * Maps a sql result set into a object.
     *
     * @param resultSet containing columns of a row
     * @return result
     * @throws Exception if something went wrong
     */
    T mapResultSet(ResultSet resultSet) throws Exception;
}

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

package org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper;

import org.eclipse.dataspaceconnector.clients.postgresql.row.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The {@link ExistsMapper} is used to map the result of SQL EXISTS queries.
 * <p>
 * Example:
 * <p>
 * SQL:
 * SELECT EXISTS(SELECT 1 FROM assets WHERE id = ?);
 * <p>
 * Resulting Table:
 * | exists |
 * | ------ |
 * |  true  |
 */
public class ExistsMapper implements RowMapper<Boolean> {

    private static final String EXISTS_COLUMN = "exists";

    @Override
    public Boolean mapRow(ResultSet resultSet) throws SQLException {
        return resultSet.getBoolean(EXISTS_COLUMN);
    }
}

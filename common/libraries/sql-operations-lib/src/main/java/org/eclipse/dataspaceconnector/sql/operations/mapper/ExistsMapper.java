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

package org.eclipse.dataspaceconnector.sql.operations.mapper;

import org.eclipse.dataspaceconnector.sql.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExistsMapper implements ResultSetMapper<Boolean> {

    private static final int EXISTS_COLUMN = 1;

    @Override
    public Boolean mapResultSet(ResultSet resultSet) throws SQLException {
        return resultSet.getBoolean(EXISTS_COLUMN);
    }
}

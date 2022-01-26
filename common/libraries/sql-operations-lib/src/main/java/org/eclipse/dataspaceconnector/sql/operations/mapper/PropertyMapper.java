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
import org.eclipse.dataspaceconnector.sql.operations.serialization.EnvelopePacker;
import org.eclipse.dataspaceconnector.sql.operations.types.Property;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PropertyMapper implements ResultSetMapper<Property> {

    private static final String KEY_COLUMN = "k";
    private static final String VALUE_COLUMN = "v";

    @Override
    public Property mapResultSet(ResultSet resultSet) throws SQLException {
        String key = resultSet.getString(KEY_COLUMN);
        Object value = EnvelopePacker.unpack(resultSet.getString(VALUE_COLUMN));

        return new Property(key, value);
    }
}

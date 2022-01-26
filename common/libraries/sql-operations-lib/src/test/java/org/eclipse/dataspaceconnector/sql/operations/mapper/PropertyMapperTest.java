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

import org.eclipse.dataspaceconnector.sql.operations.serialization.EnvelopePacker;
import org.eclipse.dataspaceconnector.sql.operations.types.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PropertyMapperTest {
    private PropertyMapper propertyMapper;

    @BeforeEach
    public void setup() {
        propertyMapper = new PropertyMapper();
    }

    @Test
    public void testMapping() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getString("k")).thenReturn("key");
        Mockito.when(resultSet.getString("v")).thenReturn(EnvelopePacker.pack("value"));

        Property result = propertyMapper.mapResultSet(resultSet);
        Assertions.assertEquals(result.getKey(), "key");
        Assertions.assertEquals(result.getValue(), "value");
    }
}

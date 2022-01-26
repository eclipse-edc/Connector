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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExistsMapperTest {

    private ExistsMapper existsMapper;

    @BeforeEach
    public void setup() {
        existsMapper = new ExistsMapper();
    }

    @Test
    public void testExistsTrue() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getBoolean(1)).thenReturn(true);

        boolean result = existsMapper.mapResultSet(resultSet);
        Assertions.assertTrue(result);
    }

    @Test
    public void testExistsFalse() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getBoolean(1)).thenReturn(false);

        boolean result = existsMapper.mapResultSet(resultSet);
        Assertions.assertFalse(result);
    }

}

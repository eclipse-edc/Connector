/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.pool.commons;

import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.DriverManagerConnectionFactory;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class DriverManagerConnectionFactoryTest {
    private static final String DS_NAME = "datasource";
    private final Connection connection = mock();
    private final DriverManagerConnectionFactory factory = new DriverManagerConnectionFactory();

    @Test
    void create() throws SQLException {
        try (var driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(eq(DS_NAME), any(Properties.class))).thenReturn(connection);
            try (var conn = factory.create(DS_NAME, new Properties())) {
                assertThat(conn).isEqualTo(connection);
            }
        }
    }

    @Test
    void create_shouldThrowException() {
        try (var driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(eq(DS_NAME), any(Properties.class)))
                    .thenThrow(SQLException.class);

            assertThatThrownBy(() -> factory.create(DS_NAME, new Properties())).isInstanceOf(EdcPersistenceException.class);
        }
    }
}

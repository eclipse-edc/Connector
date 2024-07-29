/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.sql.bootstrapper;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SqlDmlStatementRunnerTest {
    private final QueryExecutor queryExecutor = mock();
    private final DataSourceRegistry datasourceRegistry = mock();
    private final SqlDmlStatementRunner executor = new SqlDmlStatementRunner(new NoopTransactionContext(), queryExecutor, mock(), datasourceRegistry);


    @Test
    void executeSql() throws SQLException {
        var dataSourceMock = mock(DataSource.class);
        when(dataSourceMock.getConnection()).thenReturn(mock(Connection.class));
        when(datasourceRegistry.resolve(anyString())).thenReturn(dataSourceMock);

        executor.executeSql(Map.of("foosource", List.of("test-schema.sql")));

        verify(queryExecutor).execute(any(Connection.class), notNull());
    }

    @Test
    void executeSql_errorDuringGetConnection() throws SQLException {
        var dataSourceMock = mock(DataSource.class);
        when(dataSourceMock.getConnection()).thenThrow(new SQLException("test exception"));
        when(datasourceRegistry.resolve(anyString())).thenReturn(dataSourceMock);

        assertThat(executor.executeSql(Map.of("foosource", List.of("test-schema.sql")))).isFailed()
                .detail().isEqualTo("test exception");

        verifyNoInteractions(queryExecutor);
    }

    @Test
    void executeSql_datasourceNotFound() {
        when(datasourceRegistry.resolve(anyString())).thenReturn(null);
        assertThat(executor.executeSql(Map.of("foosource", List.of("test-schema.sql")))).isFailed()
                .detail().isEqualTo("No datasource found with name 'foosource'");

        verifyNoInteractions(queryExecutor);
    }

    @Test
    void executeSql_noStatementsInQueue() {
        executor.executeSql(Map.of());
        verifyNoInteractions(queryExecutor, datasourceRegistry);
    }

    @Test
    void executeSql_oneStatementFails() throws SQLException {
        var dataSourceMock = mock(DataSource.class);
        when(dataSourceMock.getConnection()).thenReturn(mock(Connection.class));
        when(datasourceRegistry.resolve(anyString())).thenReturn(dataSourceMock);

        when(queryExecutor.execute(any(Connection.class), notNull()))
                .thenReturn(1)
                .thenThrow(new EdcException("test exception"));

        assertThatThrownBy(() -> executor.executeSql(Map.of("foosource", List.of("test-schema.sql"), "barsource", List.of("test-schema.sql"))))
                .isInstanceOf(EdcException.class)
                .hasMessage("test exception");

        verify(queryExecutor, times(2)).execute(any(Connection.class), notNull());
    }
}
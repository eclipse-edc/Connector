/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.transaction.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSourceResourceTest {
    private DataSource dataSource;
    private DataSourceResource resource;
    private Connection connection;

    @Test
    void verifyGetConnectionPassword() throws SQLException {
        when(dataSource.getConnection(isA(String.class), isA(String.class))).thenReturn(connection);

        resource.start();
        var connection1 = resource.getConnection("foo", "bar");
        var connection2 = resource.getConnection("foo", "bar");

        assertThat(connection1).isSameAs(connection2);

        resource.commit();

        var connection3 = resource.getConnection("foo", "bar");

        assertThat(connection3).isNotSameAs(connection2);
    }


    @Test
    void verifyCommit() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);

        resource.start();
        resource.getConnection();
        resource.commit();

        // ensure enlisted context was removed by calling commit again; the connection should not receive a commit() invocation
        resource.commit();

        verify(connection, times(1)).commit();
        verify(connection, times(1)).close();
    }

    @Test
    void verifyCommitThrowsException() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        doThrow(new RuntimeException()).when(connection).commit();

        resource.start();
        resource.getConnection();

        assertThatThrownBy(() -> resource.commit());

        verify(connection, times(1)).commit();
        verify(connection, times(1)).close();
    }

    @Test
    void verifyRollback() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);

        resource.start();
        resource.getConnection();
        resource.rollback();

        // ensure enlisted context was removed by calling rollback again; the connection should not receive a rollback() invocation
        resource.rollback();

        verify(connection, times(1)).rollback();
        verify(connection, times(1)).close();
        verify(connection, never()).commit();
    }

    @Test
    void verifyRollbackThrowsException() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        doThrow(new RuntimeException()).when(connection).rollback();

        resource.start();
        resource.getConnection();

        assertThatThrownBy(() -> resource.rollback());

        // ensure enlisted context was removed by calling rollback again; the connection should not receive a rollback() invocation
        resource.rollback();

        verify(connection, times(1)).rollback();
        verify(connection, times(1)).close();
        verify(connection, never()).commit();
    }

    @Test
    void verifySameConnectionIsReturnedInTransaction() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);

        resource.start();

        var connection1 = resource.getConnection();
        var connection2 = resource.getConnection();

        resource.commit();

        assertThat(connection1).isSameAs(connection2);
    }

    @Test
    void verifyDifferentConnectionIsReturned() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));

        resource.start();

        var connection1 = resource.getConnection();

        resource.commit();

        resource.start();

        var connection2 = resource.getConnection();

        resource.commit();

        assertThat(connection1).isNotSameAs(connection2);
    }

    @Test
    void verifyNoEnlistedResourcesCommit() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);

        resource.start();
        resource.commit();

        verify(connection, never()).commit();   // no connection is enlisted so the commit should not be called
    }

    @BeforeEach
    void setUp() {
        connection = mock(Connection.class);
        dataSource = mock(DataSource.class);
        resource = new DataSourceResource(dataSource);
    }
}

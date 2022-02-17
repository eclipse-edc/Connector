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
 *       Daimler TSS GmbH - Initial Test
 *
 */

package org.eclipse.dataspaceconnector.sql;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

class SqlQueryExecutorTest {
    private static final String DUMMY_SQL = "<not intended to be actually evaluated>";

    @Test
    void testExecuteMutatingQuery() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(DUMMY_SQL, Statement.RETURN_GENERATED_KEYS)).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.execute()).thenReturn(false);
        Mockito.when(preparedStatement.getUpdateCount()).thenReturn(12345);

        Integer result = SqlQueryExecutor.executeQuery(connection, DUMMY_SQL);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(12345, result);
    }

    @Test
    void testExecuteSelectingQuery() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(DUMMY_SQL, Statement.RETURN_GENERATED_KEYS)).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.execute()).thenReturn(true);

        Integer result = SqlQueryExecutor.executeQuery(connection, DUMMY_SQL);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result);
    }

    @Test
    void testExecuteMutatingQueryWithResultSetMapper() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(DUMMY_SQL, Statement.RETURN_GENERATED_KEYS)).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.execute()).thenReturn(false);
        Mockito.when(preparedStatement.getUpdateCount()).thenReturn(12345);
        ResultSetMapper<?> mapper = Mockito.mock(ResultSetMapper.class);

        List<?> result = SqlQueryExecutor.executeQuery(connection, mapper, DUMMY_SQL);

        Assertions.assertNotNull(result);
    }

    @Test
    void testExecuteSelectingQueryWithResultSetMapper() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(DUMMY_SQL, Statement.RETURN_GENERATED_KEYS)).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.execute()).thenReturn(true);
        ResultSetMapper<?> mapper = Mockito.mock(ResultSetMapper.class);

        List<?> result = SqlQueryExecutor.executeQuery(connection, mapper, DUMMY_SQL);

        Assertions.assertNotNull(result);
    }

    @ParameterizedTest
    @ArgumentsSource(TestExecuteParametrizedArgumentProvider.class)
    void testExecuteParametrized(Object argument, MockitoPreparedStatementVerification verification) throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(DUMMY_SQL, Statement.RETURN_GENERATED_KEYS)).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.execute()).thenReturn(true);

        Integer result = SqlQueryExecutor.executeQuery(connection, DUMMY_SQL, argument);

        verification.verify(preparedStatement);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result);
    }


    static class TestExecuteParametrizedArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var object = new SomeRandomObject();
            var inputStream = new NullInputStream();

            return Stream.of(
                    Arguments.of(null, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setNull(1, java.sql.Types.NULL)),
                    Arguments.of(11234, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setInt(1, 11234)),
                    Arguments.of(1.234, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setDouble(1, 1.234)),
                    Arguments.of(1.234f, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setFloat(1, 1.234f)),
                    Arguments.of("string", (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setString(1, "string")),
                    Arguments.of((short) 1234, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setShort(1, (short) 1234)),
                    Arguments.of(1L, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setLong(1, 1L)),
                    Arguments.of(false, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setBoolean(1, false)),
                    Arguments.of((byte) 1, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setByte(1, (byte) 1)),
                    Arguments.of(BigDecimal.valueOf(1L), (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setBigDecimal(1, BigDecimal.valueOf(1L))),
                    Arguments.of(new Date(), (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setTimestamp(Mockito.eq(1), Mockito.any(Timestamp.class))),
                    Arguments.of("bytes".getBytes(), (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setBytes(1, "bytes".getBytes())),
                    Arguments.of(inputStream, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setBlob(1, inputStream)),
                    Arguments.of(object, (MockitoPreparedStatementVerification) (statement) -> Mockito.verify(statement).setObject(1, object))
            );
        }
    }

    @FunctionalInterface
    interface MockitoPreparedStatementVerification {
        void verify(PreparedStatement statement) throws SQLException;
    }

    private static class SomeRandomObject {
    }

    private static class NullInputStream extends InputStream {
        @Override
        public int read() {
            return 0;
        }
    }
}

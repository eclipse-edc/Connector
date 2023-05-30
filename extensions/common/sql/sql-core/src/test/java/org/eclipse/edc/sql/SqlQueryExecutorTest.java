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

package org.eclipse.edc.sql;

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
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqlQueryExecutorTest {
    private static final String DUMMY_SQL = "<not intended to be actually evaluated>";
    private final SqlQueryExecutor executor = new SqlQueryExecutor();

    @ParameterizedTest
    @ArgumentsSource(TestExecuteParametrizedArgumentProvider.class)
    void setArgumentCorrectType(Object argument, MockitoPreparedStatementVerification verification) throws SQLException {
        var connection = Mockito.mock(Connection.class);
        var preparedStatement = Mockito.mock(PreparedStatement.class);
        when(connection.prepareStatement(DUMMY_SQL, Statement.RETURN_GENERATED_KEYS)).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        executor.execute(connection, DUMMY_SQL, argument);

        verification.verify(preparedStatement);
    }

    static class TestExecuteParametrizedArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var object = new SomeRandomObject();
            var inputStream = new NullInputStream();

            return Stream.of(
                    Arguments.of(null, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setNull(1, java.sql.Types.NULL)),
                    Arguments.of(11234, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setInt(1, 11234)),
                    Arguments.of(1.234, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setDouble(1, 1.234)),
                    Arguments.of(1.234f, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setFloat(1, 1.234f)),
                    Arguments.of("string", (MockitoPreparedStatementVerification) (statement) -> verify(statement).setString(1, "string")),
                    Arguments.of((short) 1234, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setShort(1, (short) 1234)),
                    Arguments.of(1L, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setLong(1, 1L)),
                    Arguments.of(false, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setBoolean(1, false)),
                    Arguments.of((byte) 1, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setByte(1, (byte) 1)),
                    Arguments.of(BigDecimal.valueOf(1L), (MockitoPreparedStatementVerification) (statement) -> verify(statement).setBigDecimal(1, BigDecimal.valueOf(1L))),
                    Arguments.of(new Date(), (MockitoPreparedStatementVerification) (statement) -> verify(statement).setTimestamp(Mockito.eq(1), Mockito.any(Timestamp.class))),
                    Arguments.of("bytes".getBytes(), (MockitoPreparedStatementVerification) (statement) -> verify(statement).setBytes(1, "bytes".getBytes())),
                    Arguments.of(inputStream, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setBlob(1, inputStream)),
                    Arguments.of(object, (MockitoPreparedStatementVerification) (statement) -> verify(statement).setObject(1, object))
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

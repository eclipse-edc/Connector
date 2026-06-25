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

package org.eclipse.edc.sql;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

enum ArgumentHandlers implements ArgumentHandler {
    /**
     * Sets an {@code int} argument into its corresponding position of a statement
     */
    INT {
        @Override
        public boolean accepts(Object value) {
            return value instanceof Integer;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setInt(position, (int) argument);
        }
    },
    /**
     * Sets an {@code long} argument into its corresponding position of a statement
     */
    LONG {
        @Override
        public boolean accepts(Object value) {
            return value instanceof Long;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setLong(position, (long) argument);
        }
    },
    /**
     * Sets an {@code double} argument into its corresponding position of a statement
     */
    DOUBLE {
        @Override
        public boolean accepts(Object value) {
            return value instanceof Double;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setDouble(position, (double) argument);
        }
    },
    /**
     * Sets an {@code float} argument into its corresponding position of a statement
     */
    FLOAT {
        @Override
        public boolean accepts(Object value) {
            return value instanceof Float;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setFloat(position, (float) argument);
        }
    },
    /**
     * Sets an {@code short} argument into its corresponding position of a statement
     */
    SHORT {
        @Override
        public boolean accepts(Object value) {
            return value instanceof Short;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setShort(position, (short) argument);
        }
    },
    /**
     * Sets an {@code java.math.BigDecimal} argument into its corresponding position of a statement
     */
    BIG_DECIMAL {
        @Override
        public boolean accepts(Object value) {
            return value instanceof BigDecimal;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setBigDecimal(position, (BigDecimal) argument);
        }
    },
    /**
     * Sets an {@code java.lang.String} argument into its corresponding position of a statement
     */
    STRING {
        @Override
        public boolean accepts(Object value) {
            return value instanceof String;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setString(position, (String) argument);
        }
    },
    /**
     * Sets an {@code boolean} argument into its corresponding position of a statement
     */
    BOOLEAN {
        @Override
        public boolean accepts(Object value) {
            return value instanceof Boolean;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setBoolean(position, (Boolean) argument);
        }
    },
    /**
     * Sets an {@code java.util.Date} argument into its corresponding position of a statement
     */
    DATE {
        @Override
        public boolean accepts(Object value) {
            return value instanceof Date;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setTimestamp(position, new Timestamp(((Date) argument).getTime()));
        }
    },
    /**
     * Sets an {@code byte} argument into its corresponding position of a statement
     */
    BYTE {
        @Override
        public boolean accepts(Object value) {
            return value instanceof Byte;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setByte(position, (Byte) argument);
        }
    },
    /**
     * Sets an {@code byte[]} array argument into its corresponding position of a statement
     */
    BYTES {
        @Override
        public boolean accepts(Object value) {
            return value instanceof byte[];
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setBytes(position, (byte[]) argument);
        }
    },
    /**
     * Sets an {@code java.io.InputStream} argument into its corresponding position of a statement
     */
    INPUT_STREAM {
        @Override
        public boolean accepts(Object value) {
            return value instanceof InputStream;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setBlob(position, (InputStream) argument);
        }
    },
    /**
     * Sets an {@code null} argument into its corresponding position of a statement
     */
    NULL {
        @Override
        public boolean accepts(Object value) {
            return value == null;
        }

        @Override
        public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
            statement.setNull(position, java.sql.Types.NULL);
        }
    }
}

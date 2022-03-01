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

package org.eclipse.dataspaceconnector.sql.contractdefinition.schema;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

class FlywayDataSource implements DataSource {
    private final DataSource delegate;

    public FlywayDataSource(DataSource dataSource) {
        this.delegate = Objects.requireNonNull(dataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return new FlywayDataSourceConnection(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String s, String s1) throws SQLException {
        return new FlywayDataSourceConnection(delegate.getConnection(s, s1));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws SQLException {
        delegate.setLogWriter(printWriter);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> type) throws SQLException {
        return delegate.unwrap(type);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int i) throws SQLException {
        delegate.setLoginTimeout(i);
    }

    @Override
    public boolean isWrapperFor(Class<?> type) throws SQLException {
        return delegate.isWrapperFor(type);
    }


}

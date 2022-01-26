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

package org.eclipse.dataspaceconnector.sql.operations;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.UUID;
import javax.sql.DataSource;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlDataSourceExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private JdbcDataSource dataSource;

    @Override
    public void beforeAll(ExtensionContext context) {
        this.dataSource = new JdbcDataSource();
        this.dataSource.setUrl(String.format("jdbc:h2:mem:%s", UUID.randomUUID()));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        executeQuery(dataSource.getConnection(), TestPreparedStatementResourceReader.getTablesCreate());
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        executeQuery(dataSource.getConnection(), TestPreparedStatementResourceReader.getTablesDelete());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return parameterType == DataSource.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();

        if (parameterType == DataSource.class) {
            return dataSource;
        }

        throw new UnsupportedOperationException("Cannot resolve parameter of type " + parameterType.getName());
    }

}

/*
 *  Copyright (c) 2022 Daimler TSS GmbH
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

package org.eclipse.dataspaceconnector.sql.contractdefinition.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.contractdefinition.spi.SqlContractDefinitionTables;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlContractDefinitionLoader implements ContractDefinitionLoader {

    private static final String SQL_ACCEPT_CLAUSE_TEMPLATE = String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
            SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE,
            SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID,
            SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ACCESS_POLICY,
            SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_CONTRACT_POLICY,
            SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_SELECTOR);
    private final ObjectMapper objectMapper;
    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    private final TransactionContext transactionContext;

    public SqlContractDefinitionLoader(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ObjectMapper objectMapper) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public void accept(ContractDefinition definition) {
        Objects.requireNonNull(definition);

        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                executeQuery(connection, SQL_ACCEPT_CLAUSE_TEMPLATE,
                        definition.getId(),
                        objectMapper.writeValueAsString(definition.getAccessPolicy()),
                        objectMapper.writeValueAsString(definition.getContractPolicy()),
                        objectMapper.writeValueAsString(definition.getSelectorExpression()));
            } catch (Exception e) {
                throw new EdcException(e.getMessage(), e);
            }
        });

    }

    private DataSource getDataSource() {
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), String.format("DataSource %s could not be resolved", dataSourceName));
    }
    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}

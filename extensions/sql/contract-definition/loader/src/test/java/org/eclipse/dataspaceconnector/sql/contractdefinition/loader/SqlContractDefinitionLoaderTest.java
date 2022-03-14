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
 *       Daimler TSS GmbH - Initial Tests
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.loader;

import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.contractdefinition.schema.ConfigurationKeys;
import org.eclipse.dataspaceconnector.sql.contractdefinition.schema.SqlContractDefinitionTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

@ExtendWith(EdcExtension.class)
class SqlContractDefinitionLoaderTest {

    private final Map<String, String> systemProperties = new HashMap<>() {
        {
            put(ConfigurationKeys.DATASOURCE_SETTING_URL, "jdbc:h2:mem:test");
            put(ConfigurationKeys.DATASOURCE_SETTING_DRIVER_CLASS, org.h2.Driver.class.getName());
            put(ConfigurationKeys.DATASOURCE_SETTING_NAME, ConfigurationKeys.DATASOURCE_NAME);
        }
    };

    private final AtomicReference<ServiceExtensionContext> contextRef = new AtomicReference<>();

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.registerSystemExtension(ConfigurationExtension.class, (ConfigurationExtension) () -> ConfigFactory.fromMap(systemProperties));
        extension.registerSystemExtension(ServiceExtension.class, new ServiceExtension() {
            public void initialize(ServiceExtensionContext context) {
                contextRef.set(context);
            }
        });
    }

    @AfterEach
    void tearDown() {
        getTransactionContext().execute(() -> {
            try (Connection connection = getDataSourceRegistry().resolve(ConfigurationKeys.DATASOURCE_NAME).getConnection()) {
                executeQuery(connection, String.format("DELETE FROM %s", SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        contextRef.set(null);
    }

    protected DataSourceRegistry getDataSourceRegistry() {
        return contextRef.get().getService(DataSourceRegistry.class);
    }

    protected TransactionContext getTransactionContext() {
        return contextRef.get().getService(TransactionContext.class);
    }

    protected ContractDefinitionLoader getContractDefinitionLoader() {
        return contextRef.get().getService(ContractDefinitionLoader.class);
    }

    @Test
    @DisplayName("Context Loads, tables exist")
    void contextLoads() throws SQLException {
        var query = String.format("SELECT 1 FROM %s", SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE);
        executeQuery(getDataSourceRegistry().resolve(ConfigurationKeys.DATASOURCE_NAME).getConnection(), query);
    }

    @Test
    @DisplayName("Accepts and persists ContractDefinition")
    void testAcceptEntry() throws SQLException {
        var accessPolicy = Policy.Builder.newInstance()
                .id("access")
                .build();
        var contractPolicy = Policy.Builder.newInstance()
                .id("contract")
                .build();
        var expression = AssetSelectorExpression.Builder.newInstance()
                .build();
        var definition = ContractDefinition.Builder.newInstance()
                .id("id")
                .accessPolicy(accessPolicy)
                .contractPolicy(contractPolicy)
                .selectorExpression(expression)
                .build();

        getContractDefinitionLoader().accept(definition);

        var query = String.format("SELECT count(*) FROM %s WHERE %s=?",
                SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID);

        var definitionCount = executeQuery(
                getDataSourceRegistry().resolve(ConfigurationKeys.DATASOURCE_NAME).getConnection(),
                (rs) -> rs.getLong(1),
                query, "id").iterator().next();

        assertThat(definitionCount).isEqualTo(1);
    }
}
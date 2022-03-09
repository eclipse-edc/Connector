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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.contractdefinition.schema.SqlContractDefinitionTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

@ExtendWith(EdcExtension.class)
public class SqlContractDefinitionStoreTest {
    private static final String DATASOURCE_NAME = "contractdefinition";

    private final Map<String, String> systemProperties = new HashMap<>() {
        {
            put(String.format("edc.datasource.%s.url", DATASOURCE_NAME), "jdbc:h2:mem:test");
            put(String.format("edc.datasource.%s.driverClassName", DATASOURCE_NAME), org.h2.Driver.class.getName());
            put("edc.contractdefinition.datasource.name", DATASOURCE_NAME);
        }
    };

    private final AtomicReference<ServiceExtensionContext> contextRef = new AtomicReference<>();

    @BeforeEach
    void setUp(EdcExtension extension) {
        systemProperties.forEach(System::setProperty);
        extension.registerSystemExtension(ServiceExtension.class, new ServiceExtension() {
            public void initialize(ServiceExtensionContext context) {
                contextRef.set(context);
            }
        });
    }

    @AfterEach
    void tearDown() {
        getTransactionContext().execute(() -> {
            try (var connection = getDataSourceRegistry().resolve(DATASOURCE_NAME).getConnection()) {
                executeQuery(connection, String.format("DELETE FROM %s", SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        systemProperties.keySet().forEach(System::clearProperty);
        contextRef.set(null);
    }

    private ContractDefinition getContractDefinition(String id, String contractId, String policyId) {
        var accessPolicy = Policy.Builder.newInstance()
                .id(policyId)
                .build();
        var contractPolicy = Policy.Builder.newInstance()
                .id(contractId)
                .build();
        var expression = AssetSelectorExpression.Builder.newInstance()
                .build();

        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicy(accessPolicy)
                .contractPolicy(contractPolicy)
                .selectorExpression(expression)
                .build();
    }

    private ArrayList<ContractDefinition> getContractDefinitions(int count) {
        var definitions = new ArrayList<ContractDefinition>();

        for (int i = 0; i < count; i++) {
            definitions.add(getContractDefinition("id" + i, "contract" + i, "policy" + i));
        }

        return definitions;
    }

    @Test
    @DisplayName("Context Loads, tables exist")
    void contextLoads() throws SQLException {
        var query = String.format("SELECT 1 FROM %s", SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE);
        executeQuery(getDataSourceRegistry().resolve(DATASOURCE_NAME).getConnection(), query);
    }

    @Test
    @DisplayName("Save a single Contract Definition")
    void saveOne() {
        var definition = getContractDefinition("id", "contract", "policy");
        getContractDefinitionStore().save(definition);

        var definitions = getContractDefinitionStore().findAll();

        Assertions.assertNotNull(definitions);
        Assertions.assertEquals(1, definitions.size());
    }

    @Test
    @DisplayName("Save multiple Contract Definitions")
    void saveMany() {
        var definitionsCreated = getContractDefinitions(10);
        getContractDefinitionStore().save(definitionsCreated);

        var definitionsRetrieved = getContractDefinitionStore().findAll();

        Assertions.assertNotNull(definitionsRetrieved);
        Assertions.assertEquals(definitionsCreated.size(), definitionsRetrieved.size());
    }

    @Test
    @DisplayName("Update a Contract Definition")
    void updateOne() throws SQLException {
        var definition1 = getContractDefinition("id", "contract1", "policy1");
        var definition2 = getContractDefinition("id", "contract2", "policy2");

        getContractDefinitionStore().save(definition1);
        getContractDefinitionStore().update(definition2);

        var query = String.format("SELECT * FROM %s WHERE %s=?",
                SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID);

        var definitions = executeQuery(getDataSourceRegistry().resolve(DATASOURCE_NAME).getConnection(),
                SqlContractDefinitionStore::mapResultSet,
                query,
                definition1.getId());

        Assertions.assertEquals(1, definitions.size());
        Assertions.assertEquals(definition2.getContractPolicy().getUid(), definitions.get(0).getContractPolicy().getUid());
        Assertions.assertEquals(definition2.getAccessPolicy().getUid(), definitions.get(0).getAccessPolicy().getUid());
    }

    @Test
    @DisplayName("Find all contract definitions")
    void findAll() {
        var definitionsExpected = getContractDefinitions(10);
        getContractDefinitionStore().save(definitionsExpected);

        var definitionsRetrieved = getContractDefinitionStore().findAll();

        Assertions.assertEquals(definitionsExpected.size(), definitionsRetrieved.size());
    }

    @Test
    @DisplayName("Find all contract definitions with limit and offset")
    void findAllWithSpec() {
        var limit = 20;

        var definitionsExpected = getContractDefinitions(50);
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .limit(limit)
                .offset(20)
                .build();

        var definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        Assertions.assertEquals(limit, definitionsRetrieved.size());
    }

    private DataSourceRegistry getDataSourceRegistry() {
        return contextRef.get().getService(DataSourceRegistry.class);
    }

    private TransactionContext getTransactionContext() {
        return contextRef.get().getService(TransactionContext.class);
    }

    private ContractDefinitionStore getContractDefinitionStore() {
        return contextRef.get().getService(ContractDefinitionStore.class);
    }
}

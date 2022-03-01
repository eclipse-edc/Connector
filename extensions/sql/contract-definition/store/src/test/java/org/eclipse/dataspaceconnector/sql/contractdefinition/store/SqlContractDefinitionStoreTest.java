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

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.contractdefinition.schema.SqlContractDefinitionTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlContractDefinitionStoreTest extends AbstractSqlContractDefinitionStoreTest {
    private static final String DATASOURCE_NAME = "contractdefinition";

    private final Map<String, String> systemProperties = new HashMap<>() {
        {
            put(String.format("edc.datasource.%s.url", DATASOURCE_NAME), "jdbc:h2:mem:test");
            put(String.format("edc.datasource.%s.driverClassName", DATASOURCE_NAME), org.h2.Driver.class.getName());
            put("edc.contractdefinition.datasource.name", DATASOURCE_NAME);
        }
    };

    @BeforeEach
    void setUp() {
        systemProperties.forEach(System::setProperty);
    }

    @AfterEach
    void tearDown() {
        getTransactionContext().execute(() -> {
            try (Connection connection = getDataSourceRegistry().resolve(DATASOURCE_NAME).getConnection()) {
                executeQuery(connection, String.format("DELETE FROM %s", SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        systemProperties.keySet().forEach(System::clearProperty);
    }

    private ContractDefinition getContractDefinition(String id, String contractId, String policyId) {
        Policy accessPolicy = Policy.Builder.newInstance()
                .id(policyId)
                .build();
        Policy contractPolicy = Policy.Builder.newInstance()
                .id(contractId)
                .build();
        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .build();

        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicy(accessPolicy)
                .contractPolicy(contractPolicy)
                .selectorExpression(expression)
                .build();
    }

    private ArrayList<ContractDefinition> getContractDefinitions(int count) {
        ArrayList<ContractDefinition> definitions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            definitions.add(getContractDefinition("id" + i, "contract" + i, "policy" + i));
        }

        return definitions;
    }

    @Test
    @DisplayName("Context Loads, tables exist")
    void contextLoads() throws SQLException {
        String query = String.format("SELECT 1 FROM %s", SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE);
        executeQuery(getDataSourceRegistry().resolve(DATASOURCE_NAME).getConnection(), query);
    }

    @Test
    @DisplayName("Save a single Contract Definition")
    void saveOne() {
        ContractDefinition definition = getContractDefinition("id", "contract", "policy");
        getContractDefinitionStore().save(definition);

        Collection<ContractDefinition> definitions = getContractDefinitionStore().findAll();

        Assertions.assertNotNull(definitions);
        Assertions.assertEquals(1, definitions.size());
    }

    @Test
    @DisplayName("Save multiple Contract Definitions")
    void saveMany() {
        List<ContractDefinition> definitionsCreated = getContractDefinitions(10);
        getContractDefinitionStore().save(definitionsCreated);

        Collection<ContractDefinition> definitionsRetrieved = getContractDefinitionStore().findAll();

        Assertions.assertNotNull(definitionsRetrieved);
        Assertions.assertEquals(definitionsCreated.size(), definitionsRetrieved.size());
    }

    @Test
    @DisplayName("Update a Contract Definition")
    void updateOne() throws SQLException {
        ContractDefinition definition1 = getContractDefinition("id", "contract1", "policy1");
        ContractDefinition definition2 = getContractDefinition("id", "contract2", "policy2");

        getContractDefinitionStore().save(definition1);
        getContractDefinitionStore().update(definition2);

        String query = String.format("SELECT * FROM %s WHERE %s=?",
                SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID);

        List<ContractDefinition> definitions = executeQuery(getDataSourceRegistry().resolve(DATASOURCE_NAME).getConnection(),
                SqlContractDefinitionStore.ContractDefinitionMapper.INSTANCE,
                query,
                definition1.getId());

        Assertions.assertEquals(1, definitions.size());
        Assertions.assertEquals(definition2.getContractPolicy().getUid(), definitions.get(0).getContractPolicy().getUid());
        Assertions.assertEquals(definition2.getAccessPolicy().getUid(), definitions.get(0).getAccessPolicy().getUid());
    }

    @Test
    @DisplayName("Find all contract definitions")
    void findAll() {
        List<ContractDefinition> definitionsExpected = getContractDefinitions(10);
        getContractDefinitionStore().save(definitionsExpected);

        Collection<ContractDefinition> definitionsRetrieved = getContractDefinitionStore().findAll();

        Assertions.assertEquals(definitionsExpected.size(), definitionsRetrieved.size());
    }

    @Test
    @DisplayName("Find all contract definitions with limit and offset")
    void findAllWithSpec() {
        int limit = 20;

        List<ContractDefinition> definitionsExpected = getContractDefinitions(50);
        getContractDefinitionStore().save(definitionsExpected);

        QuerySpec spec = QuerySpec.Builder.newInstance()
                .limit(limit)
                .offset(20)
                .build();

        Collection<ContractDefinition> definitionsRetrieved = getContractDefinitionStore().findAll(spec).collect(Collectors.toList());

        Assertions.assertEquals(limit, definitionsRetrieved.size());
    }
}

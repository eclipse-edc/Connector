/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.store.sql.contractdefinition;


import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.ContractDefinitionStoreTestBase;
import org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.TestFunctions;
import org.eclipse.edc.connector.store.sql.contractdefinition.schema.BaseSqlDialectStatements;
import org.eclipse.edc.connector.store.sql.contractdefinition.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class PostgresContractDefinitionStoreTest extends ContractDefinitionStoreTestBase {

    private final BaseSqlDialectStatements statements = new PostgresDialectStatements();

    private SqlContractDefinitionStore sqlContractDefinitionStore;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {

        var typeManager = new TypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        sqlContractDefinitionStore = new SqlContractDefinitionStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), statements, typeManager.getMapper(), queryExecutor);
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getContractDefinitionTable() + " CASCADE");
    }

    @Test
    @DisplayName("Verify empty result when query contains invalid keys")
    void findAll_queryByInvalidKey() {

        var definitionsExpected = TestFunctions.createContractDefinitions(20);
        saveContractDefinitions(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("notexist", "=", "somevalue")))
                .build();

        assertThatThrownBy(() -> getContractDefinitionStore().findAll(spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

    }

    // Override in PG since it does not have the field mapping
    @Test
    void findAll_verifySorting_invalidProperty() {
        range(0, 10).mapToObj(i -> TestFunctions.createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);
        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        assertThatThrownBy(() -> getContractDefinitionStore().findAll(query)).isInstanceOf(IllegalArgumentException.class);
    }

    @Override
    protected ContractDefinitionStore getContractDefinitionStore() {
        return sqlContractDefinitionStore;
    }

    @Override
    protected boolean supportsCollectionQuery() {
        return true;
    }

    @Override
    protected boolean supportsCollectionIndexQuery() {
        return false;
    }

}

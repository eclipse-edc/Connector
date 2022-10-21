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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;


import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.contract.offer.store.ContractDefinitionStoreTestBase;
import org.eclipse.dataspaceconnector.contract.offer.store.TestFunctions;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.PostgresqlStoreSetupExtension;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.BaseSqlDialectStatements;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.postgres.PostgresDialectStatements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.contract.offer.store.TestFunctions.createContractDefinition;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

@PostgresqlDbIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class PostgresContractDefinitionStoreTest extends ContractDefinitionStoreTestBase {


    private final BaseSqlDialectStatements statements = new PostgresDialectStatements();

    private SqlContractDefinitionStore sqlContractDefinitionStore;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension) throws IOException, SQLException {

        var typeManager = new TypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        sqlContractDefinitionStore = new SqlContractDefinitionStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), statements, typeManager.getMapper());
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) throws SQLException {
        extension.runQuery("DROP TABLE " + statements.getContractDefinitionTable() + " CASCADE");
    }

    @Test
    @DisplayName("Context Loads, tables exist")
    void contextLoads(PostgresqlStoreSetupExtension extension) {
        var query = String.format("SELECT 1 FROM %s", statements.getContractDefinitionTable());

        var result = executeQuery(extension.getConnection(), query);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Verify empty result when query contains invalid keys")
    void findAll_queryByInvalidKey() {

        var definitionsExpected = TestFunctions.createContractDefinitions(20);
        getContractDefinitionStore().save(definitionsExpected);

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
        IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);
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

    @Override
    protected boolean supportsSortOrder() {
        return true;
    }

}

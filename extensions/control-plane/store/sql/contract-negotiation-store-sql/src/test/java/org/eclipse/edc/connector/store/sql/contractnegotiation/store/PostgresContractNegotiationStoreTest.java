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

package org.eclipse.edc.connector.store.sql.contractnegotiation.store;

import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.ContractNegotiationStoreTestBase;
import org.eclipse.edc.connector.store.sql.contractnegotiation.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.lease.testfixtures.LeaseUtil;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createContract;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createContractBuilder;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createNegotiation;

/**
 * This test aims to verify those parts of the contract negotiation store, that are specific to Postgres, e.g. JSON
 * query operators.
 */
@PostgresqlDbIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class PostgresContractNegotiationStoreTest extends ContractNegotiationStoreTestBase {

    private SqlContractNegotiationStore store;
    private LeaseUtil leaseUtil;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension) throws SQLException, IOException {

        var statements = new PostgresDialectStatements();
        TypeManager manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        store = new SqlContractNegotiationStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), manager.getMapper(), statements, CONNECTOR_NAME, Clock.systemUTC());

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
        leaseUtil = new LeaseUtil(extension.getTransactionContext(), extension::getConnection, statements, Clock.systemUTC());
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) throws Exception {
        var dialect = new PostgresDialectStatements();
        extension.runQuery("DROP TABLE " + dialect.getContractNegotiationTable() + " CASCADE");
        extension.runQuery("DROP TABLE " + dialect.getContractAgreementTable() + " CASCADE");
        extension.runQuery("DROP TABLE " + dialect.getLeaseTableName() + " CASCADE");
    }

    @Test
    void query_byAgreementId() {

        var agreement1 = createContract("agr1");
        var agreement2 = createContract("agr2");
        var negotiation1 = createNegotiation("neg1", agreement1);
        var negotiation2 = createNegotiation("neg2", agreement2);
        store.updateOrCreate(negotiation1);
        store.updateOrCreate(negotiation2);

        var expression = "contractAgreement.id = agr1";
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        var result = store.findAllNegotiations(query).collect(Collectors.toList());

        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsOnly(negotiation1);
    }

    @Test
    void query_byPolicyAssignee() {

        var policy = Policy.Builder.newInstance()
                .assignee("test-assignee")
                .assigner("test-assigner")
                .permission(Permission.Builder.newInstance()
                        .target("")
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .constraint(AtomicConstraint.Builder.newInstance()
                                .leftExpression(new LiteralExpression("foo"))
                                .operator(Operator.EQ)
                                .rightExpression(new LiteralExpression("bar"))
                                .build())
                        .build())
                .build();

        var agreement1 = createContractBuilder("agr1").policy(policy).build();
        var agreement2 = createContractBuilder("agr2").policy(policy).build();
        var negotiation1 = createNegotiation("neg1", agreement1);
        var negotiation2 = createNegotiation("neg2", agreement2);
        store.updateOrCreate(negotiation1);
        store.updateOrCreate(negotiation2);

        var expression = "contractAgreement.policy.assignee = test-assignee";
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        var result = store.findAllNegotiations(query).collect(Collectors.toList());

        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(negotiation1, negotiation2);
    }

    @Test
    void query_invalidKey_shouldThrowException() {
        var agreement1 = createContract("agr1");
        var negotiation1 = createNegotiation("neg1", agreement1);
        store.updateOrCreate(negotiation1);

        var expression = "contractAgreement.notexist = agr1";
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        assertThatThrownBy(() -> store.findAllNegotiations(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");
    }

    @Test
    void query_invalidKeyInJson() {
        var agreement1 = createContract("agr1");
        var negotiation1 = createNegotiation("neg1", agreement1);
        store.updateOrCreate(negotiation1);

        var expression = "contractAgreement.policy.permissions.notexist = foobar";
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        assertThat(store.findAllNegotiations(query)).isEmpty();
    }

    @Test
    void queryAgreements_withQuerySpec() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.createContractId(UUID.randomUUID().toString()))
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.updateOrCreate(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().filter("assetId = asset-2").build();
        var all = store.findAllAgreements(query);

        assertThat(all).hasSize(1);
    }

    @Test
    void queryAgreements_withQuerySpec_invalidOperand() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.createContractId(UUID.randomUUID().toString()))
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.updateOrCreate(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().filter("notexistprop = asset-2").build();
        assertThatThrownBy(() -> store.findAllAgreements(query));
    }

    @Test
    void queryAgreements_withQuerySpec_noFilter() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.createContractId(UUID.randomUUID().toString()))
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.updateOrCreate(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().offset(2).limit(2).build();
        assertThat(store.findAllAgreements(query)).hasSize(2);
    }

    @Test
    void queryAgreements_withQuerySpec_invalidValue() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.createContractId(UUID.randomUUID().toString()))
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.updateOrCreate(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().filter("assetId = notexist").build();
        assertThat(store.findAllAgreements(query)).isEmpty();
    }

    @Test
    void create_and_cancel_contractAgreement() {
        var negotiationId = "test-cn1";
        var negotiation = createNegotiation(negotiationId);
        store.updateOrCreate(negotiation);

        // now add the agreement
        var agreement = createContract("test-ca1");
        var updatedNegotiation = createNegotiation(negotiationId, agreement);

        store.updateOrCreate(updatedNegotiation);
        assertThat(store.findAllAgreements(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(agreement);

        // cancel the agreement
        updatedNegotiation.transitionError("Cancelled");
        store.updateOrCreate(updatedNegotiation);
    }

    @Override
    protected SqlContractNegotiationStore getContractNegotiationStore() {
        return store;
    }

    @Override
    protected void lockEntity(String negotiationId, String owner, Duration duration) {
        leaseUtil.leaseEntity(negotiationId, owner, duration);
    }

    @Override
    protected boolean isLockedBy(String negotiationId, String owner) {
        return leaseUtil.isLeased(negotiationId, owner);
    }

}

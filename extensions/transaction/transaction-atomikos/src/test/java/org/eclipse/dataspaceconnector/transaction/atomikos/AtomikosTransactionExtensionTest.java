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
package org.eclipse.dataspaceconnector.transaction.atomikos;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.dataspaceconnector.transaction.atomikos.JdbcTestFixtures.createAtomikosConfig;
import static org.eclipse.dataspaceconnector.transaction.atomikos.JdbcTestFixtures.createDataSourceConfig;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies transaction setup by executing committed and rollback transactions against an in-memory H2 database.
 */
class AtomikosTransactionExtensionTest {
    AtomikosTransactionExtension extension;

    @Test
    void verifyEndToEndTransactions() {
        var extensionContext = mock(ServiceExtensionContext.class);
        when(extensionContext.getConnectorId()).thenReturn(randomUUID().toString());
        when(extensionContext.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(TransactionManagerConfigurationKeys.LOGGING, "false")));

        when(extensionContext.getConfig(isA(String.class))).thenAnswer(a -> createDataSourceConfig());
        when(extensionContext.getConfig()).thenAnswer(a -> createAtomikosConfig());
        when(extensionContext.getMonitor()).thenReturn(new ConsoleMonitor());

        var dsRegistry = new DataSourceRegistry[1];
        doAnswer(invocation -> {
            dsRegistry[0] = invocation.getArgument(1);
            return null;
        }).when(extensionContext).registerService(eq(DataSourceRegistry.class), isA(DataSourceRegistry.class));

        var transactionContext = new TransactionContext[1];
        doAnswer(invocation -> {
            transactionContext[0] = invocation.getArgument(1);
            return null;
        }).when(extensionContext).registerService(eq(TransactionContext.class), isA(TransactionContext.class));

        extension.initialize(extensionContext);
        extension.start();

        transactionContext[0].execute(() -> {
            try (var conn = dsRegistry[0].resolve("default").getConnection()) {
                Statement s1 = conn.createStatement();
                s1.execute("DROP ALL OBJECTS");
                s1.execute("CREATE TABLE Foo (id number)");
                s1.execute("INSERT into Foo values (1)");
            } catch (SQLException e) {
                throw new EdcException(e);
            }
        });

        // verify committed data available in separate transaction
        int[] numberOfResults = new int[1];
        transactionContext[0].execute(() -> {
            try (var conn = dsRegistry[0].resolve("default").getConnection()) {
                Statement s1 = conn.createStatement();
                var results = s1.executeQuery("SELECT * FROM Foo where id = 1");
                numberOfResults[0] = results.last() ? results.getRow() : 0;
            } catch (SQLException e) {
                throw new EdcException(e);
            }
        });

        assertThat(numberOfResults[0]).isEqualTo(1);

        // verify rollback on exception in a nested block

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() ->
                        transactionContext[0].execute(() -> {
                            try (var conn = dsRegistry[0].resolve("default").getConnection()) {
                                Statement s1 = conn.createStatement();
                                s1.execute("INSERT into Foo values (2)");
                                transactionContext[0].execute(() -> {
                                    throw new RuntimeException();
                                });
                            } catch (SQLException e) {
                                throw new EdcException(e);
                            }
                        }));

        transactionContext[0].execute(() -> {
            try (var conn = dsRegistry[0].resolve("default").getConnection()) {
                Statement s1 = conn.createStatement();
                var results = s1.executeQuery("SELECT * FROM Foo where id <= 2");
                numberOfResults[0] = results.last() ? results.getRow() : 0;
            } catch (SQLException e) {
                throw new EdcException(e);
            }
        });

        // the second rollsback; only one row should be present
        assertThat(numberOfResults[0]).isEqualTo(1);

        extension.shutdown();
    }

    @BeforeEach
    void setUp() {
        extension = new AtomikosTransactionExtension();
    }
}

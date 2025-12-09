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
 *       Daimler TSS GmbH - Initial Extension Test
 *
 */

package org.eclipse.edc.sql.pool.commons;

import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.ConnectionFactory;
import org.eclipse.edc.sql.datasource.ConnectionPoolDataSource;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTIONS_MAX_IDLE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTIONS_MAX_TOTAL;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTIONS_MIN_IDLE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_ON_BORROW;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_ON_CREATE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_ON_RETURN;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_QUERY;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_WHILE_IDLE;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class CommonsConnectionPoolServiceExtensionTest {
    private static final String DS_1_NAME = "ds1";
    private final DataSourceRegistry dataSourceRegistry = mock();
    private final ConnectionFactory connectionFactory = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(DataSourceRegistry.class, dataSourceRegistry);
        context.registerService(Vault.class, new InMemoryVault(mock(), null));
        context.registerService(ConnectionFactory.class, connectionFactory);
    }

    @ParameterizedTest
    @ArgumentsSource(ConfigProvider.class)
    void initialize_withConfig(Map<String, String> configuration, ThrowingConsumer<CommonsConnectionPoolConfig> checker,
                               boolean isEnv,
                               CommonsConnectionPoolServiceExtension extension, ServiceExtensionContext context) {
        var config = isEnv ? ConfigFactory.fromEnvironment(configuration) : ConfigFactory.fromMap(configuration);
        when(context.getConfig(EDC_DATASOURCE_PREFIX)).thenReturn(config);

        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(ConnectionPoolDataSource.class);
        verify(dataSourceRegistry).register(eq(DS_1_NAME), captor.capture());
        assertThat(captor.getAllValues()).hasSize(1).first()
                .extracting("connectionPool").asInstanceOf(type(CommonsConnectionPool.class))
                .extracting(CommonsConnectionPool::getPoolConfig).satisfies(checker);
    }

    @Test
    void initialize_fromVault(CommonsConnectionPoolServiceExtension extension, ServiceExtensionContext context) {
        when(context.getConfig(EDC_DATASOURCE_PREFIX))
                .thenReturn(ConfigFactory.fromMap(Map.of("ds1.name", "ds1")));
        var vault = context.getService(Vault.class);

        vault.storeSecret("edc.datasource." + DS_1_NAME + ".user", "test-user");
        vault.storeSecret("edc.datasource." + DS_1_NAME + ".password", "test-pwd");
        vault.storeSecret("edc.datasource." + DS_1_NAME + ".url", "jdbc://whatever");

        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(ConnectionPoolDataSource.class);
        verify(dataSourceRegistry).register(eq(DS_1_NAME), captor.capture());
        assertThat(captor.getAllValues()).hasSize(1).first()
                .extracting("connectionPool").asInstanceOf(type(CommonsConnectionPool.class))
                .satisfies(pool -> {
                    assertThatThrownBy(pool::getConnection).isInstanceOf(EdcException.class); //we need this only to invoke the connection factory
                    verify(connectionFactory).create(eq("jdbc://whatever"), argThat(p ->
                            p.size() == 3 &&
                                    p.containsValue("test-pwd") &&
                                    p.containsValue("test-user")));
                });
    }

    @Test
    void initialize_fromVault_shouldOverrideConfig(CommonsConnectionPoolServiceExtension extension, ServiceExtensionContext context) {
        when(context.getConfig(EDC_DATASOURCE_PREFIX))
                .thenReturn(ConfigFactory.fromMap(
                        Map.of("ds1.name", "ds1",
                                "ds1.user", "this-should-be-ignored",
                                "ds1.password", "this-as-well")));
        var vault = context.getService(Vault.class);

        vault.storeSecret("edc.datasource." + DS_1_NAME + ".user", "test-user");
        vault.storeSecret("edc.datasource." + DS_1_NAME + ".password", "test-pwd");
        vault.storeSecret("edc.datasource." + DS_1_NAME + ".url", "jdbc://whatever");

        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(ConnectionPoolDataSource.class);
        verify(dataSourceRegistry).register(eq(DS_1_NAME), captor.capture());
        assertThat(captor.getAllValues()).hasSize(1).first()
                .extracting("connectionPool").asInstanceOf(type(CommonsConnectionPool.class))
                .satisfies(pool -> {
                    assertThatThrownBy(pool::getConnection).isInstanceOf(EdcException.class); //we need this only to invoke the connection factory
                    verify(connectionFactory).create(eq("jdbc://whatever"), argThat(p ->
                            p.size() == 3 &&
                                    p.containsValue("test-pwd") &&
                                    p.containsValue("test-user")));
                });
    }

    static class ConfigProvider implements ArgumentsProvider {

        private final Map<String, String> defaultConfig = Map.of(DS_1_NAME + ".url", DS_1_NAME);

        private final Map<String, String> configuration = Map.of(
                DS_1_NAME + ".url", DS_1_NAME,
                DS_1_NAME + "." + POOL_CONNECTION_TEST_ON_CREATE, "false",
                DS_1_NAME + "." + POOL_CONNECTION_TEST_ON_BORROW, "false",
                DS_1_NAME + "." + POOL_CONNECTION_TEST_ON_RETURN, "true",
                DS_1_NAME + "." + POOL_CONNECTION_TEST_WHILE_IDLE, "true",
                DS_1_NAME + "." + POOL_CONNECTION_TEST_QUERY, "SELECT foo FROM bar;",
                DS_1_NAME + "." + POOL_CONNECTIONS_MIN_IDLE, "10",
                DS_1_NAME + "." + POOL_CONNECTIONS_MAX_IDLE, "10",
                DS_1_NAME + "." + POOL_CONNECTIONS_MAX_TOTAL, "10");


        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            ThrowingConsumer<CommonsConnectionPoolConfig> checkDefault = this::checkDefault;
            ThrowingConsumer<CommonsConnectionPoolConfig> checkWithConfig = this::checkWithConfig;

            var envConfiguration = configuration.entrySet().stream()
                    .map(it -> Map.entry(it.getKey().toUpperCase().replace(".", "_"), it.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return Stream.of(
                    Arguments.of(defaultConfig, checkDefault, false),
                    Arguments.of(configuration, checkWithConfig, false),
                    Arguments.of(envConfiguration, checkWithConfig, true)
            );
        }

        private void checkDefault(CommonsConnectionPoolConfig cfg) {
            assertThat(cfg.getTestConnectionOnCreate()).isTrue();
            assertThat(cfg.getTestConnectionOnBorrow()).isTrue();
            assertThat(cfg.getTestConnectionOnReturn()).isFalse();
            assertThat(cfg.getTestConnectionWhileIdle()).isFalse();
            assertThat(cfg.getTestQuery()).isEqualTo("SELECT 1;");
            assertThat(cfg.getMinIdleConnections()).isEqualTo(1);
            assertThat(cfg.getMaxIdleConnections()).isEqualTo(4);
            assertThat(cfg.getMaxTotalConnections()).isEqualTo(8);
        }

        private void checkWithConfig(CommonsConnectionPoolConfig cfg) {
            assertThat(cfg.getTestConnectionOnCreate()).isFalse();
            assertThat(cfg.getTestConnectionOnBorrow()).isFalse();
            assertThat(cfg.getTestConnectionOnReturn()).isTrue();
            assertThat(cfg.getTestConnectionWhileIdle()).isTrue();
            assertThat(cfg.getTestQuery()).isEqualTo("SELECT foo FROM bar;");
            assertThat(cfg.getMinIdleConnections()).isEqualTo(10);
            assertThat(cfg.getMaxIdleConnections()).isEqualTo(10);
            assertThat(cfg.getMaxTotalConnections()).isEqualTo(10);
        }

    }

}

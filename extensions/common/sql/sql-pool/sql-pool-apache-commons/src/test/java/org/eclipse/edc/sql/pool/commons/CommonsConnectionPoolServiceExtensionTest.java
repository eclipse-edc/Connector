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
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.DEPRACATED_POOL_MAX_IDLE_CONNECTIONS;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.DEPRACATED_POOL_MAX_TOTAL_CONNECTIONS;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.DEPRACATED_POOL_MIN_IDLE_CONNECTIONS;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.DEPRACATED_POOL_TEST_CONNECTION_ON_BORROW;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.DEPRACATED_POOL_TEST_CONNECTION_ON_CREATE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.DEPRACATED_POOL_TEST_CONNECTION_ON_RETURN;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.DEPRACATED_POOL_TEST_CONNECTION_WHILE_IDLE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.DEPRACATED_POOL_TEST_QUERY;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTIONS_MAX_IDLE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTIONS_MAX_TOTAL;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTIONS_MIN_IDLE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_ON_BORROW;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_ON_CREATE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_ON_RETURN;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_QUERY;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension.POOL_CONNECTION_TEST_WHILE_IDLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//sometimes hangs and causes the test to never finish.
@ExtendWith(DependencyInjectionExtension.class)
class CommonsConnectionPoolServiceExtensionTest {
    private static final String DS_1_NAME = "ds1";
    private final DataSourceRegistry dataSourceRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(DataSourceRegistry.class, dataSourceRegistry);
    }

    @ParameterizedTest
    @ArgumentsSource(ConfigProvider.class)
    void initialize_withConfig(Map<String, String> configuration, ThrowingConsumer<CommonsConnectionPoolConfig> checker,
                               boolean isEnv,
                               CommonsConnectionPoolServiceExtension extension, ServiceExtensionContext context) {
        Config config;
        if (isEnv) {
            config = ConfigFactory.fromEnvironment(configuration);
        } else {
            config = ConfigFactory.fromMap(configuration);
        }
        when(context.getConfig(EDC_DATASOURCE_PREFIX)).thenReturn(config);

        extension.initialize(context);

        verify(dataSourceRegistry).register(eq(DS_1_NAME), any());

        assertThat(extension.getCommonsConnectionPools()).hasSize(1).first()
                .extracting(CommonsConnectionPool::getPoolConfig)
                .satisfies(checker);
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


        private final Map<String, String> deprecatedConfig = Map.of(
                DS_1_NAME + ".url", DS_1_NAME,
                DS_1_NAME + "." + DEPRACATED_POOL_TEST_CONNECTION_ON_CREATE, "false",
                DS_1_NAME + "." + DEPRACATED_POOL_TEST_CONNECTION_ON_BORROW, "false",
                DS_1_NAME + "." + DEPRACATED_POOL_TEST_CONNECTION_ON_RETURN, "true",
                DS_1_NAME + "." + DEPRACATED_POOL_TEST_CONNECTION_WHILE_IDLE, "true",
                DS_1_NAME + "." + DEPRACATED_POOL_TEST_QUERY, "SELECT foo FROM bar;",
                DS_1_NAME + "." + DEPRACATED_POOL_MIN_IDLE_CONNECTIONS, "10",
                DS_1_NAME + "." + DEPRACATED_POOL_MAX_IDLE_CONNECTIONS, "10",
                DS_1_NAME + "." + DEPRACATED_POOL_MAX_TOTAL_CONNECTIONS, "10");

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
                    Arguments.of(envConfiguration, checkWithConfig, true),
                    Arguments.of(deprecatedConfig, checkWithConfig, false)
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

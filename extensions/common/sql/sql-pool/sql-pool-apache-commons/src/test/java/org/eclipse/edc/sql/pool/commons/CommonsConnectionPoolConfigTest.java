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
 *       Daimler TSS GmbH - Initial Test
 *
 */

package org.eclipse.edc.sql.pool.commons;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(DependencyInjectionExtension.class)
class CommonsConnectionPoolConfigTest {

    private static final String DATA_SOURCE_NAME = "testdatasource";

    private static final String SETTING_URL = String.format("%s.%s.%s",
            CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX,
            DATA_SOURCE_NAME,
            CommonsConnectionPoolConfigKeys.URL);
    private static final String SETTING_URL_VALUE = "jdbc:postgresql://test:5432/test";
    private CommonsConnectionPoolServiceExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataSourceRegistry.class, mock(DataSourceRegistry.class));
        extension = factory.constructInstance(CommonsConnectionPoolServiceExtension.class);
    }

    @Test
    void initialize_canLoadMaxIdleConnectionsSetting() {
        var settingMaxTotalConnections = String.format("%s.%s.%s",
                CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX,
                DATA_SOURCE_NAME,
                CommonsConnectionPoolConfigKeys.POOL_MAX_IDLE_CONNECTIONS);
        var settingMaxIdleConnectionsValue = 1;
        var partitionedConfigSpy = mockLoadSettingsBehaviour(
                settingMaxTotalConnections,
                Integer.toString(settingMaxIdleConnectionsValue));

        verify(partitionedConfigSpy).getInteger(eq(CommonsConnectionPoolConfigKeys.POOL_MAX_IDLE_CONNECTIONS), isNull());

        var maxTotalConnections =
                partitionedConfigSpy.getInteger(CommonsConnectionPoolConfigKeys.POOL_MAX_IDLE_CONNECTIONS, null);
        assertEquals(settingMaxIdleConnectionsValue, maxTotalConnections);
    }

    @Test
    void initialize_canLoadMaxTotalConnectionsSetting() {
        var settingMaxTotalConnections = String.format("%s.%s.%s",
                CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX,
                DATA_SOURCE_NAME,
                CommonsConnectionPoolConfigKeys.POOL_MAX_TOTAL_CONNECTIONS);
        var settingMaxTotalConnectionsValue = 1;
        var partitionedConfigSpy = mockLoadSettingsBehaviour(
                settingMaxTotalConnections,
                Integer.toString(settingMaxTotalConnectionsValue));

        verify(partitionedConfigSpy).getInteger(eq(CommonsConnectionPoolConfigKeys.POOL_MAX_TOTAL_CONNECTIONS), isNull());

        var maxTotalConnections =
                partitionedConfigSpy.getInteger(CommonsConnectionPoolConfigKeys.POOL_MAX_TOTAL_CONNECTIONS, null);
        assertEquals(settingMaxTotalConnectionsValue, maxTotalConnections);
    }

    @Test
    void initialize_canLoadMinIdleConnectionsSetting() {
        var poolSettingKey = CommonsConnectionPoolConfigKeys.POOL_MIN_IDLE_CONNECTIONS;
        var fullSettingKey = String.format("%s.%s.%s",
                CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX,
                DATA_SOURCE_NAME,
                poolSettingKey);
        var expectedSettingValue = 1;
        var partitionedConfigSpy = mockLoadSettingsBehaviour(
                fullSettingKey,
                Integer.toString(expectedSettingValue));

        verify(partitionedConfigSpy).getInteger(eq(poolSettingKey), isNull());

        var resultSettingValue =
                partitionedConfigSpy.getInteger(poolSettingKey, null);
        assertEquals(expectedSettingValue, resultSettingValue);
    }

    @Test
    void initialize_canLoadTestConnectionOnBorrowSetting() {
        var poolSettingKey = CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_BORROW;
        var fullSettingKey = String.format("%s.%s.%s",
                CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX,
                DATA_SOURCE_NAME,
                poolSettingKey);
        var expectedSettingValue = "true";
        var partitionedConfigSpy = mockLoadSettingsBehaviour(
                fullSettingKey,
                expectedSettingValue);

        verify(partitionedConfigSpy).getString(eq(poolSettingKey), isNull());

        var resultSettingValue =
                partitionedConfigSpy.getString(poolSettingKey, null);
        assertEquals(expectedSettingValue, resultSettingValue);
    }

    @Test
    void initialize_canLoadTestConnectionOnCreateSetting() {
        var poolSettingKey = CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_CREATE;
        var fullSettingKey = String.format("%s.%s.%s",
                CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX,
                DATA_SOURCE_NAME,
                poolSettingKey);
        var expectedSettingValue = "true";
        var partitionedConfigSpy = mockLoadSettingsBehaviour(
                fullSettingKey,
                expectedSettingValue);

        verify(partitionedConfigSpy).getString(eq(poolSettingKey), isNull());

        var resultSettingValue =
                partitionedConfigSpy.getString(poolSettingKey, null);
        assertEquals(expectedSettingValue, resultSettingValue);
    }

    @Test
    void initialize_canLoadTestConnectionOnReturnSetting() {
        var poolSettingKey = CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_RETURN;
        var fullSettingKey = String.format("%s.%s.%s",
                CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX,
                DATA_SOURCE_NAME,
                poolSettingKey);
        var expectedSettingValue = "true";
        var partitionedConfigSpy = mockLoadSettingsBehaviour(
                fullSettingKey,
                expectedSettingValue);

        verify(partitionedConfigSpy).getString(eq(poolSettingKey), isNull());

        var resultSettingValue =
                partitionedConfigSpy.getString(poolSettingKey, null);
        assertEquals(expectedSettingValue, resultSettingValue);
    }

    @Test
    void initialize_canLoadTestConnectionWhileIdleSetting() {
        var poolSettingKey = CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_WHILE_IDLE;
        var fullSettingKey = String.format("%s.%s.%s",
                CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX,
                DATA_SOURCE_NAME,
                poolSettingKey);
        var expectedSettingValue = "true";
        var partitionedConfigSpy = mockLoadSettingsBehaviour(
                fullSettingKey,
                expectedSettingValue);

        verify(partitionedConfigSpy).getString(eq(poolSettingKey), isNull());

        var resultSettingValue =
                partitionedConfigSpy.getString(poolSettingKey, null);
        assertEquals(expectedSettingValue, resultSettingValue);
    }

    @Test
    void initialize_canLoadTestQuerySetting() {
        var poolSettingKey = CommonsConnectionPoolConfigKeys.POOL_TEST_QUERY;
        var fullSettingKey = String.format("%s.%s.%s",
                CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX,
                DATA_SOURCE_NAME,
                poolSettingKey);
        var expectedSettingValue = "query";
        var partitionedConfigSpy = mockLoadSettingsBehaviour(
                fullSettingKey,
                expectedSettingValue);

        verify(partitionedConfigSpy).getString(eq(poolSettingKey), isNull());

        var resultSettingValue =
                partitionedConfigSpy.getString(poolSettingKey, null);
        assertEquals(expectedSettingValue, resultSettingValue);
    }

    @SuppressWarnings("unchecked")
    private Config mockLoadSettingsBehaviour(String inputSettingPath, String expectedSettingValue) {
        var configMap = Map.of(
                inputSettingPath, expectedSettingValue,
                SETTING_URL, SETTING_URL_VALUE);
        var config = ConfigFactory.fromMap(configMap);
        var datasourceConfig =
                config.getConfig(CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX);
        var partitionList = datasourceConfig.partition().collect(Collectors.toList());
        var partitionedConfig = partitionList.stream()
                .findFirst()
                .orElseThrow(() -> new EdcException("Could not find partitioned config"));

        var context = mock(ServiceExtensionContext.class);
        var parentConfigMock = mock(Config.class);
        var mockStream = (Stream<Config>) mock(Stream.class);
        var mockPartitionList = mock(List.class);
        var mockIterator = mock(Iterator.class);
        var partitionedConfigSpy = Mockito.spy(partitionedConfig);

        when(context.getConfig(CommonsConnectionPoolServiceExtension.EDC_DATASOURCE_PREFIX)).thenReturn(parentConfigMock);
        when(parentConfigMock.partition()).thenReturn(mockStream);
        when(mockStream.collect(any())).thenReturn(mockPartitionList);
        when(mockIterator.hasNext()).thenReturn(true, false);
        when(mockPartitionList.iterator()).thenReturn(mockIterator);
        when(mockIterator.next()).thenReturn(partitionedConfigSpy);

        extension.initialize(context);

        return partitionedConfigSpy;
    }

    @Test
    void testDefaults() {
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();

        Assertions.assertEquals(4, commonsConnectionPoolConfig.getMaxIdleConnections());
        Assertions.assertEquals(8, commonsConnectionPoolConfig.getMaxTotalConnections());
        Assertions.assertEquals(1, commonsConnectionPoolConfig.getMinIdleConnections());
        Assertions.assertTrue(commonsConnectionPoolConfig.getTestConnectionOnBorrow());
        Assertions.assertTrue(commonsConnectionPoolConfig.getTestConnectionOnCreate());
        Assertions.assertTrue(commonsConnectionPoolConfig.getTestConnectionOnReturn());
        Assertions.assertFalse(commonsConnectionPoolConfig.getTestConnectionWhileIdle());
        Assertions.assertEquals("SELECT 1;", commonsConnectionPoolConfig.getTestQuery());
    }

    @Test
    void test() {
        int minIdleConnections = 1;
        int maxIdleConnections = 2;
        int maxTotalConnections = 3;
        boolean testConnectionOnBorrow = true;
        boolean testConnectionOnCreate = false;
        boolean testConnectionWhileIdle = true;
        boolean testConnectionOnReturn = false;
        String testQuery = "testquery";

        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance()
                .maxIdleConnections(maxIdleConnections)
                .maxTotalConnections(maxTotalConnections)
                .minIdleConnections(minIdleConnections)
                .testConnectionOnBorrow(testConnectionOnBorrow)
                .testConnectionOnCreate(testConnectionOnCreate)
                .testConnectionOnReturn(testConnectionOnReturn)
                .testConnectionWhileIdle(testConnectionWhileIdle)
                .testQuery(testQuery)
                .build();

        Assertions.assertEquals(maxIdleConnections, commonsConnectionPoolConfig.getMaxIdleConnections());
        Assertions.assertEquals(maxTotalConnections, commonsConnectionPoolConfig.getMaxTotalConnections());
        Assertions.assertEquals(minIdleConnections, commonsConnectionPoolConfig.getMinIdleConnections());
        Assertions.assertEquals(testConnectionOnBorrow, commonsConnectionPoolConfig.getTestConnectionOnBorrow());
        Assertions.assertEquals(testConnectionOnCreate, commonsConnectionPoolConfig.getTestConnectionOnCreate());
        Assertions.assertEquals(testConnectionOnReturn, commonsConnectionPoolConfig.getTestConnectionOnReturn());
        Assertions.assertEquals(testConnectionWhileIdle, commonsConnectionPoolConfig.getTestConnectionWhileIdle());
        Assertions.assertEquals(testQuery, commonsConnectionPoolConfig.getTestQuery());
    }
}

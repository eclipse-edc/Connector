/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.configuration;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DataSourceNameTest {

    private final Monitor monitor = mock();

    @Test
    void shouldReturnDefaultDatasource_whenNoConfiguration() {
        var config = ConfigFactory.empty();

        var datasourceName = DataSourceName.getDataSourceName("key", "deprecatedKey", config, monitor);

        assertThat(datasourceName).isEqualTo(DataSourceRegistry.DEFAULT_DATASOURCE);
        verifyNoInteractions(monitor);
    }

    @Test
    void shouldReturnDatasource_whenIsInConfiguration() {
        var config = ConfigFactory.fromMap(Map.of("key", "value"));

        var datasourceName = DataSourceName.getDataSourceName("key", "deprecatedKey", config, monitor);

        assertThat(datasourceName).isEqualTo("value");
        verifyNoInteractions(monitor);
    }

    @Test
    void shouldLogWarning_whenDeprecatedKeyIsUsed() {
        var config = ConfigFactory.fromMap(Map.of("deprecatedKey", "value"));

        var datasourceName = DataSourceName.getDataSourceName("key", "deprecatedKey", config, monitor);

        assertThat(datasourceName).isEqualTo("value");
        verify(monitor).warning(anyString());
    }
}

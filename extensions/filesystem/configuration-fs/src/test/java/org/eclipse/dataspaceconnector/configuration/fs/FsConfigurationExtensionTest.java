/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.configuration.fs;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FsConfigurationExtensionTest {
    private FsConfigurationExtension configurationExtension;
    private ServiceExtensionContext context;

    @BeforeEach
    void setUp() throws URISyntaxException {
        Path location = Paths.get(getClass().getClassLoader().getResource("edc-configuration.properties").toURI());

        configurationExtension = new FsConfigurationExtension(location);

        context = mock(ServiceExtensionContext.class);
        when(context.getMonitor()).thenReturn(mock(Monitor.class));

        configurationExtension.initialize(context.getMonitor());
    }

    @Test
    void verifyResolution() {
        var config = configurationExtension.getConfig();

        assertThat(config.getString("testkey1")).isEqualTo("testvalue1");
        assertThat(config.getString("not.there", null)).isEqualTo(null);
    }

}

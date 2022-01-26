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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

        assertEquals("testvalue1", configurationExtension.getSetting("testkey1"));
        assertNull(configurationExtension.getSetting("notthere"));
        verify(context).getMonitor();
    }

    @Test
    void getSettingWithPrefix_groupingLevel1() {
        var all = configurationExtension.getSettingsWithPrefix("edc.datasource");
        assertThat(all).hasSize(9);

    }

    @Test
    void getSettingWithPrefix_groupingLevel2() {
        var prefix = "edc.datasource.default";
        var all = configurationExtension.getSettingsWithPrefix(prefix);


        assertThat(all).hasSize(3)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        prefix + ".user", "test-user",
                        prefix + ".password", "test-pwd",
                        prefix + ".driverClassName", "org.testcompany.testDriver.Driver"));

        prefix = "edc.datasource.another";
        all = configurationExtension.getSettingsWithPrefix(prefix);
        assertThat(all).hasSize(6)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        prefix + ".user", "test-user-2",
                        prefix + ".password", "test-pwd-2",
                        prefix + ".sub.property", "foo",
                        prefix + ".sub.property2", "bar",
                        prefix + ".driverClassName", "org.testcompany.another.Driver",
                        prefix + ".specialProperty", "specialValue"));
    }

    @Test
    void getSettingWithPrefix_groupingLevel3() {

        var prefix = "edc.datasource.another.sub";
        var all = configurationExtension.getSettingsWithPrefix(prefix);
        assertThat(all).hasSize(2)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        prefix + ".property", "foo",
                        prefix + ".property2", "bar"));
    }

}

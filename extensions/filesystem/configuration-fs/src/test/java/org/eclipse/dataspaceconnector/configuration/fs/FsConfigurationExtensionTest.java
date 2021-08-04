/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
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

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FsConfigurationExtensionTest {
    private FsConfigurationExtension configurationExtension;

    @Test
    void verifyResolution() {
        ServiceExtensionContext context = niceMock(ServiceExtensionContext.class);
        expect(context.getMonitor()).andReturn(niceMock(Monitor.class));
        replay(context);
        configurationExtension.initialize(context.getMonitor());

        assertEquals("testvalue1", configurationExtension.getSetting("testkey1"));
        assertNull(configurationExtension.getSetting("notthere"));
    }

    @BeforeEach
    void setUp() throws URISyntaxException {
        Path location = Paths.get(getClass().getClassLoader().getResource("edc-configuration.properties").toURI());

        configurationExtension = new FsConfigurationExtension(location);
    }

}

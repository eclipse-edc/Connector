/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.configuration.fs;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
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
        final ServiceExtensionContext context = niceMock(ServiceExtensionContext.class);
        expect(context.getMonitor()).andReturn(niceMock(Monitor.class));
        replay(context);
        configurationExtension.initialize(context.getMonitor());

        assertEquals("testvalue1", configurationExtension.getSetting("testkey1"));
        assertNull(configurationExtension.getSetting("notthere"));
    }

    @BeforeEach
    void setUp() throws URISyntaxException {
        @SuppressWarnings("ConstantConditions") Path location = Paths.get(getClass().getClassLoader().getResource("dagx-configuration.properties").toURI());

        configurationExtension = new FsConfigurationExtension(location);
    }

}

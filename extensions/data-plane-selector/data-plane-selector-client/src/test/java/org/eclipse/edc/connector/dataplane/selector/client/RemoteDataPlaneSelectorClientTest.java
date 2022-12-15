/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.client;

import org.eclipse.edc.connector.dataplane.selector.api.DataplaneSelectorApiController;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.jersey.JerseyConfiguration;
import org.eclipse.edc.web.jersey.JerseyRestService;
import org.eclipse.edc.web.jetty.JettyConfiguration;
import org.eclipse.edc.web.jetty.JettyService;
import org.eclipse.edc.web.jetty.PortMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createInstance;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RemoteDataPlaneSelectorClientTest {

    private static final String BASE_URL = "http://localhost:%d/api/v1/dataplane/instances";
    private static int port;
    private static JettyService jetty;
    private static DataPlaneSelectorService serviceMock;
    private static ConsoleMonitor monitor;
    private static JettyConfiguration config;
    private static TypeManager typeManager;
    private RemoteDataPlaneSelectorClient client;

    @BeforeAll
    public static void prepare() {
        typeManager = new TypeManager();
        typeManager.registerTypes(DataPlaneInstance.class);
        port = TestUtils.getFreePort();
        monitor = new ConsoleMonitor();
        config = new JettyConfiguration(null, null);
        config.portMapping(new PortMapping("dataplane", port, "/api/v1/dataplane"));
    }

    @AfterEach
    void teardown() {
        jetty.shutdown();
    }

    @BeforeEach
    void setUp() {
        jetty = startRestApi();

        var url = format(BASE_URL, port);
        client = new RemoteDataPlaneSelectorClient(testHttpClient(), url, typeManager.getMapper());
    }

    @Test
    void getAll() {

        when(serviceMock.getAll()).thenReturn(List.of(createInstance("test-inst1"), createInstance("test-inst2")));
        var result = client.getAll();
        assertThat(result).hasSize(2).extracting(DataPlaneInstance::getId).containsExactlyInAnyOrder("test-inst1", "test-inst2");
    }

    @Test
    void find() {
        var expected = createInstance("some-instance");
        when(serviceMock.select(any(), any())).thenReturn(expected);
        var result = client.find(DataAddress.Builder.newInstance().type("test1").build(), DataAddress.Builder.newInstance().type("test1").build());
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);

    }

    @Test
    void find_withStrategy() {
        var expected = createInstance("some-instance");
        when(serviceMock.select(any(), any(), eq("test-strategy"))).thenReturn(expected);
        var result = client.find(DataAddress.Builder.newInstance().type("test1").build(), DataAddress.Builder.newInstance().type("test1").build(), "test-strategy");
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    private JettyService startRestApi() {
        //set REST API
        serviceMock = mock(DataPlaneSelectorService.class);
        var controller = new DataplaneSelectorApiController(serviceMock);
        var jetty = new JettyService(config, monitor);


        var jerseyService = new JerseyRestService(jetty, typeManager, mock(JerseyConfiguration.class), monitor);
        jetty.start();

        jerseyService.registerResource("dataplane", controller);
        jerseyService.start();
        return jetty;
    }
}

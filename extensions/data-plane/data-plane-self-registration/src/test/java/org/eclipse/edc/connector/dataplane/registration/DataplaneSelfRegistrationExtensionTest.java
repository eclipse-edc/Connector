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

package org.eclipse.edc.connector.dataplane.registration;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataplaneSelfRegistrationExtensionTest {

    private final DataPlaneSelectorService dataPlaneSelectorService = mock();
    private final ControlApiUrl controlApiUrl = mock();
    private final PipelineService pipelineService = mock();
    private final EndpointDataReferenceServiceRegistry endpointDataReferenceServiceRegistry = mock();
    private final HealthCheckService healthCheckService = mock();
    private final ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(DataPlaneSelectorService.class, dataPlaneSelectorService);
        context.registerService(ControlApiUrl.class, controlApiUrl);
        context.registerService(PipelineService.class, pipelineService);
        context.registerService(EndpointDataReferenceServiceRegistry.class, endpointDataReferenceServiceRegistry);
        var monitor = mock(Monitor.class);
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        context.registerService(Monitor.class, monitor);
        context.registerService(HealthCheckService.class, healthCheckService);
        context.registerService(ResourceDefinitionGeneratorManager.class, resourceDefinitionGeneratorManager);
    }

    @Test
    void shouldRegisterInstanceAtStartup(DataplaneSelfRegistrationExtension extension, ServiceExtensionContext context) throws MalformedURLException {
        when(context.getComponentId()).thenReturn("componentId");
        when(controlApiUrl.get()).thenReturn(URI.create("http://control/api/url"));
        when(pipelineService.supportedSinkTypes()).thenReturn(Set.of("sinkType", "anotherSinkType"));
        when(pipelineService.supportedSourceTypes()).thenReturn(Set.of("sourceType", "anotherSourceType"));
        when(endpointDataReferenceServiceRegistry.supportedDestinationTypes()).thenReturn(Set.of("pullDestType", "anotherPullDestType"));
        when(endpointDataReferenceServiceRegistry.supportedResponseTypes()).thenReturn(Set.of("responseType", "anotherResponseType"));
        when(resourceDefinitionGeneratorManager.sourceTypes()).thenReturn(Set.of("supportedSourceProvisionType"));
        when(resourceDefinitionGeneratorManager.destinationTypes()).thenReturn(Set.of("supportedDestinationProvisionType"));
        when(dataPlaneSelectorService.addInstance(any())).thenReturn(ServiceResult.success());

        extension.initialize(context);
        extension.start();

        var captor = ArgumentCaptor.forClass(DataPlaneInstance.class);
        verify(dataPlaneSelectorService).addInstance(captor.capture());
        var dataPlaneInstance = captor.getValue();
        assertThat(dataPlaneInstance.getId()).isEqualTo("componentId");
        assertThat(dataPlaneInstance.getUrl()).isEqualTo(new URL("http://control/api/url/v1/dataflows"));
        assertThat(dataPlaneInstance.getAllowedSourceTypes()).containsExactlyInAnyOrder("sourceType", "anotherSourceType", "supportedSourceProvisionType");
        assertThat(dataPlaneInstance.getAllowedTransferTypes())
                .containsExactlyInAnyOrder("anotherPullDestType-PULL-anotherResponseType",
                        "anotherSinkType-PUSH-anotherResponseType",
                        "anotherPullDestType-PULL",
                        "anotherSinkType-PUSH-responseType",
                        "anotherSinkType-PUSH",
                        "pullDestType-PULL",
                        "anotherPullDestType-PULL-responseType",
                        "pullDestType-PULL-anotherResponseType",
                        "sinkType-PUSH-anotherResponseType",
                        "pullDestType-PULL-responseType",
                        "sinkType-PUSH-responseType",
                        "sinkType-PUSH");
        assertThat(dataPlaneInstance.getDestinationProvisionTypes()).containsOnly("supportedDestinationProvisionType");

        verify(healthCheckService).addStartupStatusProvider(any());
        verify(healthCheckService).addLivenessProvider(any());
        verify(healthCheckService).addReadinessProvider(any());
    }

    @Test
    void shouldNotStart_whenRegistrationFails(DataplaneSelfRegistrationExtension extension, ServiceExtensionContext context) {
        when(controlApiUrl.get()).thenReturn(URI.create("http://control/api/url"));
        when(dataPlaneSelectorService.addInstance(any())).thenReturn(ServiceResult.conflict("cannot register"));

        extension.initialize(context);

        assertThatThrownBy(extension::start).isInstanceOf(EdcException.class);
    }

    @Test
    void shouldNotUnregisterInstanceAtShutdown(DataplaneSelfRegistrationExtension extension, ServiceExtensionContext context) {
        when(context.getComponentId()).thenReturn("componentId");
        when(dataPlaneSelectorService.unregister(any())).thenReturn(ServiceResult.success());
        extension.initialize(context);

        extension.shutdown();

        verify(dataPlaneSelectorService, never()).unregister(any());
    }

    @Test
    void shouldUnregisterInstanceAtShutdown_whenConfigured(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getComponentId()).thenReturn("componentId");
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("edc.data.plane.self.unregistration", "true")));
        when(dataPlaneSelectorService.unregister(any())).thenReturn(ServiceResult.success());

        var ext = objectFactory.constructInstance(DataplaneSelfRegistrationExtension.class);
        ext.initialize(context);

        ext.shutdown();

        verify(dataPlaneSelectorService).unregister("componentId");
    }
}

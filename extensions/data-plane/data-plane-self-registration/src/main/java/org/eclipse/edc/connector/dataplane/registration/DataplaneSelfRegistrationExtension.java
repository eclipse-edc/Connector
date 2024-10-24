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

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.eclipse.edc.spi.system.health.ReadinessProvider;
import org.eclipse.edc.spi.system.health.StartupStatusProvider;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.connector.dataplane.registration.DataplaneSelfRegistrationExtension.NAME;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;

@Extension(NAME)
public class DataplaneSelfRegistrationExtension implements ServiceExtension {

    public static final boolean DEFAULT_SELF_UNREGISTRATION = false;
    public static final String NAME = "Dataplane Self Registration";
    @Setting(value = "Enable data-plane un-registration at shutdown (not suggested for clustered environments)", type = "boolean", defaultValue = DEFAULT_SELF_UNREGISTRATION + "")
    static final String SELF_UNREGISTRATION = "edc.data.plane.self.unregistration";
    private final AtomicBoolean isRegistered = new AtomicBoolean(false);
    private final AtomicReference<String> registrationError = new AtomicReference<>("Data plane self registration not complete");
    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;
    @Inject
    private ControlApiUrl controlApiUrl;
    @Inject
    private PipelineService pipelineService;
    @Inject
    private PublicEndpointGeneratorService publicEndpointGeneratorService;
    @Inject
    private HealthCheckService healthCheckService;

    private ServiceExtensionContext context;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;
    }

    @Override
    public void start() {
        var transferTypes = Stream.concat(
                toTransferTypes(PULL, publicEndpointGeneratorService.supportedDestinationTypes(), publicEndpointGeneratorService.supportedResponseTypes()),
                toTransferTypes(PUSH, pipelineService.supportedSinkTypes(), publicEndpointGeneratorService.supportedResponseTypes())
        );

        var instance = DataPlaneInstance.Builder.newInstance()
                .id(context.getComponentId())
                .url(controlApiUrl.get().toString() + "/v1/dataflows")
                .allowedSourceTypes(pipelineService.supportedSourceTypes())
                .allowedDestTypes(pipelineService.supportedSinkTypes())
                .allowedTransferType(transferTypes.collect(toSet()))
                .build();

        var monitor = context.getMonitor().withPrefix("DataPlaneHealthCheck");
        var check = new DataPlaneHealthCheck();
        healthCheckService.addReadinessProvider(check);
        healthCheckService.addLivenessProvider(check);
        healthCheckService.addStartupStatusProvider(check);

        monitor.debug("Initiate data plane registration.");
        dataPlaneSelectorService.addInstance(instance)
                .onSuccess(it -> {
                    monitor.info("data plane registered to control plane");
                    isRegistered.set(true);
                })
                .onFailure(f -> registrationError.set(f.getFailureDetail()))
                .orElseThrow(f -> new EdcException("Cannot register data plane to the control plane: " + f.getFailureDetail()));
    }

    @Override
    public void shutdown() {
        if (context.getConfig().getBoolean(SELF_UNREGISTRATION, DEFAULT_SELF_UNREGISTRATION)) {
            dataPlaneSelectorService.unregister(context.getComponentId())
                    .onSuccess(it -> context.getMonitor().info("data plane successfully unregistered"))
                    .onFailure(failure -> context.getMonitor().severe("error during data plane de-registration. %s: %s"
                            .formatted(failure.getReason(), failure.getFailureDetail())));
        }
    }

    private @NotNull Stream<String> toTransferTypes(FlowType pull, Set<String> types, Set<String> responseTypes) {
        if (responseTypes.isEmpty()) {
            return types.stream().map(it -> "%s-%s".formatted(it, pull));
        }
        return responseTypes.stream().flatMap(responseType -> types.stream().map(it -> "%s-%s/%s".formatted(it, pull, responseType)));
    }

    private class DataPlaneHealthCheck implements LivenessProvider, ReadinessProvider, StartupStatusProvider {

        @Override
        public HealthCheckResult get() {
            return HealthCheckResult.Builder.newInstance()
                    .component(NAME)
                    .success(isRegistered.get(), registrationError.get())
                    .build();
        }
    }
}

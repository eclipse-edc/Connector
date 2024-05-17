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

import org.eclipse.edc.connector.controlplane.transfer.spi.callback.ControlApiUrl;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.connector.dataplane.registration.DataplaneSelfRegistrationExtension.NAME;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;

@Extension(NAME)
public class DataplaneSelfRegistrationExtension implements ServiceExtension {

    public static final String NAME = "Dataplane Self Registration";

    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;

    @Inject
    private ControlApiUrl controlApiUrl;

    @Inject
    private PipelineService pipelineService;

    @Inject
    private PublicEndpointGeneratorService publicEndpointGeneratorService;
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
                toTransferTypes(PULL, publicEndpointGeneratorService.supportedDestinationTypes()),
                toTransferTypes(PUSH, pipelineService.supportedSinkTypes())
        );

        var instance = DataPlaneInstance.Builder.newInstance()
                .id(context.getRuntimeId())
                .url(controlApiUrl.get().toString() + "/v1/dataflows")
                .allowedSourceTypes(pipelineService.supportedSourceTypes())
                .allowedDestTypes(pipelineService.supportedSinkTypes())
                .allowedTransferType(transferTypes.collect(toSet()))
                .build();

        dataPlaneSelectorService.addInstance(instance)
                .onSuccess(it -> context.getMonitor().info("data-plane registered to control-plane"))
                .orElseThrow(f -> new EdcException("Cannot register data-plane to the control-plane: " + f.getFailureDetail()));
    }

    @Override
    public void shutdown() {
        dataPlaneSelectorService.delete(context.getRuntimeId())
                .onSuccess(it -> context.getMonitor().info("data-plane successfully unregistered"))
                .onFailure(failure -> context.getMonitor().severe("error during data-plane un-registration. %s: %s"
                        .formatted(failure.getReason(), failure.getFailureDetail())));
    }

    private @NotNull Stream<String> toTransferTypes(FlowType pull, Set<String> types) {
        return types.stream().map(it -> "%s-%s".formatted(it, pull));
    }
}

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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartController;
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartControllerSettings;
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartControllerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartControllerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandlerSettings;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandlerSettings;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandlerSettings;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandlerSettings;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandlerSettings;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandlerSettings;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.util.ErrorResult;
import org.eclipse.dataspaceconnector.ids.core.configuration.SettingResolver;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.service.DataCatalogService;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * ServiceExtension providing IDS multipart related API controllers
 */
public final class IdsMultipartApiServiceExtension implements ServiceExtension {
    private static final String NAME = "IDS Multipart API extension";

    private static final String[] REQUIRES = {
            IdentityService.FEATURE,
            "edc:ids:core",
            "edc:ids:transform:v1"
    };

    private static final String[] PROVIDES = {
            "edc:ids:api:multipart:endpoint:v1"
    };

    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of(REQUIRES);
    }

    @Override
    public Set<String> provides() {
        return Set.of(PROVIDES);
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        registerControllers(serviceExtensionContext);

        monitor.info(String.format("Initialized %s", NAME));
    }

    @Override
    public void start() {
        monitor.info(String.format("Started %s", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown %s", NAME));
    }

    private void registerControllers(ServiceExtensionContext serviceExtensionContext) {
        WebService webService = serviceExtensionContext.getService(WebService.class);
        IdentityService identityService = serviceExtensionContext.getService(IdentityService.class);
        DataCatalogService dataCatalogService = serviceExtensionContext.getService(DataCatalogService.class);
        ConnectorService connectorService = serviceExtensionContext.getService(ConnectorService.class);
        AssetIndex assetIndex = serviceExtensionContext.getService(AssetIndex.class);
        TransformerRegistry transformerRegistry = serviceExtensionContext.getService(TransformerRegistry.class);

        SettingResolver settingResolver = new SettingResolver(serviceExtensionContext);

        // create settings factories
        ArtifactDescriptionRequestHandlerSettingsFactory artifactDescriptionRequestHandlerSettingsFactory = new ArtifactDescriptionRequestHandlerSettingsFactory(settingResolver);
        DataCatalogDescriptionRequestHandlerSettingsFactory dataCatalogDescriptionRequestHandlerSettingsFactory = new DataCatalogDescriptionRequestHandlerSettingsFactory(settingResolver);
        RepresentationDescriptionRequestHandlerSettingsFactory representationDescriptionRequestHandlerSettingsFactory = new RepresentationDescriptionRequestHandlerSettingsFactory(settingResolver);
        ResourceDescriptionRequestHandlerSettingsFactory resourceDescriptionRequestHandlerSettingsFactory = new ResourceDescriptionRequestHandlerSettingsFactory(settingResolver);
        ConnectorDescriptionRequestHandlerSettingsFactory connectorDescriptionRequestHandlerSettingsFactory = new ConnectorDescriptionRequestHandlerSettingsFactory(settingResolver);
        MultipartControllerSettingsFactory multipartControllerSettingsFactory = new MultipartControllerSettingsFactory(settingResolver);
        DescriptionHandlerSettingsFactory descriptionHandlerSettingsFactory = new DescriptionHandlerSettingsFactory(settingResolver);

        // get settings factory result
        ArtifactDescriptionRequestHandlerSettingsFactoryResult artifactDescriptionRequestHandlerSettingsFactoryResult = artifactDescriptionRequestHandlerSettingsFactory.getSettingsResult();
        DataCatalogDescriptionRequestHandlerSettingsFactoryResult dataCatalogDescriptionRequestHandlerSettingsFactoryResult = dataCatalogDescriptionRequestHandlerSettingsFactory.getSettingsResult();
        RepresentationDescriptionRequestHandlerSettingsFactoryResult representationDescriptionRequestHandlerSettingsFactoryResult = representationDescriptionRequestHandlerSettingsFactory.getSettingsResult();
        ResourceDescriptionRequestHandlerSettingsFactoryResult resourceDescriptionRequestHandlerSettingsFactoryResult = resourceDescriptionRequestHandlerSettingsFactory.getSettingsResult();
        ConnectorDescriptionRequestHandlerSettingsFactoryResult connectorDescriptionRequestHandlerSettingsFactoryResult = connectorDescriptionRequestHandlerSettingsFactory.getSettingsResult();
        MultipartControllerSettingsFactoryResult multipartControllerSettingsFactoryResult = multipartControllerSettingsFactory.createRejectionMessageFactorySettings();
        DescriptionHandlerSettingsFactoryResult descriptionHandlerSettingsFactoryResult = descriptionHandlerSettingsFactory.getSettingsResult();

        // throw on settings factory errors
        throwOnError(artifactDescriptionRequestHandlerSettingsFactoryResult, dataCatalogDescriptionRequestHandlerSettingsFactoryResult, representationDescriptionRequestHandlerSettingsFactoryResult,
                resourceDescriptionRequestHandlerSettingsFactoryResult, connectorDescriptionRequestHandlerSettingsFactoryResult, multipartControllerSettingsFactoryResult, descriptionHandlerSettingsFactoryResult);

        // validate settings not null
        ArtifactDescriptionRequestHandlerSettings artifactDescriptionRequestHandlerSettings = Objects.requireNonNull(artifactDescriptionRequestHandlerSettingsFactoryResult.getSettings());
        DataCatalogDescriptionRequestHandlerSettings dataCatalogDescriptionRequestHandlerSettings = Objects.requireNonNull(dataCatalogDescriptionRequestHandlerSettingsFactoryResult.getSettings());
        RepresentationDescriptionRequestHandlerSettings representationDescriptionRequestHandlerSettings = Objects.requireNonNull(representationDescriptionRequestHandlerSettingsFactoryResult.getSettings());
        ResourceDescriptionRequestHandlerSettings resourceDescriptionRequestHandlerSettings = Objects.requireNonNull(resourceDescriptionRequestHandlerSettingsFactoryResult.getSettings());
        ConnectorDescriptionRequestHandlerSettings connectorDescriptionRequestHandlerSettings = Objects.requireNonNull(connectorDescriptionRequestHandlerSettingsFactoryResult.getSettings());
        MultipartControllerSettings multipartControllerSettings = Objects.requireNonNull(multipartControllerSettingsFactoryResult.getSettings());
        DescriptionHandlerSettings descriptionHandlerSettings = Objects.requireNonNull(descriptionHandlerSettingsFactoryResult.getSettings());

        // create description request handlers
        ArtifactDescriptionRequestHandler artifactDescriptionRequestHandler = new ArtifactDescriptionRequestHandler(monitor, artifactDescriptionRequestHandlerSettings, assetIndex, transformerRegistry);
        DataCatalogDescriptionRequestHandler dataCatalogDescriptionRequestHandler = new DataCatalogDescriptionRequestHandler(monitor, dataCatalogDescriptionRequestHandlerSettings, dataCatalogService, transformerRegistry);
        RepresentationDescriptionRequestHandler representationDescriptionRequestHandler = new RepresentationDescriptionRequestHandler(monitor, representationDescriptionRequestHandlerSettings, assetIndex, transformerRegistry);
        ResourceDescriptionRequestHandler resourceDescriptionRequestHandler = new ResourceDescriptionRequestHandler(monitor, resourceDescriptionRequestHandlerSettings, assetIndex, transformerRegistry);
        ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler = new ConnectorDescriptionRequestHandler(monitor, connectorDescriptionRequestHandlerSettings, connectorService, transformerRegistry);

        // create request handler
        DescriptionHandler descriptionHandler = new DescriptionHandler(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler);

        List<Handler> handlers = new LinkedList<>();
        handlers.add(descriptionHandler);

        // create & register controller
        MultipartController multipartController = new MultipartController(multipartControllerSettings, identityService, handlers);
        webService.registerController(multipartController);
    }

    private void throwOnError(ErrorResult... errorResults) {
        List<String> errors = new ArrayList<>();
        Arrays.stream(errorResults).map(ErrorResult::getErrors).distinct().forEach(errors::addAll);

        if (!errors.isEmpty()) {
            throw new EdcException(String.format("Invalid setting(s) in IDS Multipart Extension: %s", String.join(", ", errors)));
        }
    }
}

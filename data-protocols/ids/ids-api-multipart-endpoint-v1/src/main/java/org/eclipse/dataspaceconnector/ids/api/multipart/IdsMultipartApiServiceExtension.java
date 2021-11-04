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
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartControllerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartControllerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandlerSettingsFactoryResult;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandlerSettingsFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandlerSettingsFactoryResult;
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

import java.util.LinkedList;
import java.util.List;
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
        List<Handler> handlers = new LinkedList<>();

        // Register the top-level Multipart JAX-RS controller
        MultipartController multipartController = createMultipartController(settingResolver, identityService, handlers);
        webService.registerController(multipartController);

        // Create and register the IDS Self-Description-Handler
        DescriptionHandler descriptionHandler = createDescriptionHandler(
                settingResolver,
                connectorService,
                dataCatalogService,
                assetIndex,
                transformerRegistry
        );
        handlers.add(descriptionHandler);
    }

    private DescriptionHandler createDescriptionHandler(
            SettingResolver settingResolver,
            ConnectorService connectorService,
            DataCatalogService dataCatalogService,
            AssetIndex assetIndex,
            TransformerRegistry transformerRegistry) {
        DescriptionHandlerSettingsFactory descriptionHandlerSettingsFactory = new DescriptionHandlerSettingsFactory(settingResolver);
        DescriptionHandlerSettingsFactoryResult descriptionHandlerSettingsFactoryResult = descriptionHandlerSettingsFactory.getSettingsResult();

        if (!descriptionHandlerSettingsFactoryResult.getErrors().isEmpty()) {
            throw new EdcException(String.format("Could not set up DescriptionHandler: %s", String.join(", ", descriptionHandlerSettingsFactoryResult.getErrors())));
        }

        ArtifactDescriptionRequestHandler artifactDescriptionRequestHandler = createArtifactDescriptionRequestHandler(settingResolver, assetIndex, transformerRegistry);
        DataCatalogDescriptionRequestHandler dataCatalogDescriptionRequestHandler = createDataCatalogDescriptionRequestHandler(settingResolver, dataCatalogService, transformerRegistry);
        RepresentationDescriptionRequestHandler representationDescriptionRequestHandler = createRepresentationDescriptionRequestHandler(settingResolver, assetIndex, transformerRegistry);
        ResourceDescriptionRequestHandler resourceDescriptionRequestHandler = createResourceDescriptionRequestHandler(settingResolver, assetIndex, transformerRegistry);
        ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler = createConnectorDescriptionRequestHandler(settingResolver, connectorService, transformerRegistry);

        return new DescriptionHandler(
                monitor,
                descriptionHandlerSettingsFactoryResult.getSettings(),
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler);
    }

    private ConnectorDescriptionRequestHandler createConnectorDescriptionRequestHandler(
            SettingResolver settingResolver,
            ConnectorService connectorService,
            TransformerRegistry transformerRegistry) {
        ConnectorDescriptionRequestHandlerSettingsFactory connectorDescriptionRequestHandlerSettingsFactory = new ConnectorDescriptionRequestHandlerSettingsFactory(settingResolver);
        ConnectorDescriptionRequestHandlerSettingsFactoryResult connectorDescriptionRequestHandlerSettingsFactoryResult = connectorDescriptionRequestHandlerSettingsFactory.getSettingsResult();

        if (!connectorDescriptionRequestHandlerSettingsFactoryResult.getErrors().isEmpty()) {
            throw new EdcException(String.format("Could not set up ConnectorDescriptionRequestHandler: %s", String.join(", ", connectorDescriptionRequestHandlerSettingsFactoryResult.getErrors())));
        }

        return new ConnectorDescriptionRequestHandler(
                monitor,
                connectorDescriptionRequestHandlerSettingsFactoryResult.getSettings(),
                connectorService,
                transformerRegistry
        );
    }

    private ResourceDescriptionRequestHandler createResourceDescriptionRequestHandler(
            SettingResolver settingResolver,
            AssetIndex assetIndex,
            TransformerRegistry transformerRegistry) {
        ResourceDescriptionRequestHandlerSettingsFactory resourceDescriptionRequestHandlerSettingsFactory = new ResourceDescriptionRequestHandlerSettingsFactory(settingResolver);
        ResourceDescriptionRequestHandlerSettingsFactoryResult resourceDescriptionRequestHandlerSettingsFactoryResult = resourceDescriptionRequestHandlerSettingsFactory.getSettingsResult();

        if (!resourceDescriptionRequestHandlerSettingsFactoryResult.getErrors().isEmpty()) {
            throw new EdcException(String.format("Could not set up ResourceDescriptionRequestHandler: %s", String.join(", ", resourceDescriptionRequestHandlerSettingsFactoryResult.getErrors())));
        }

        return new ResourceDescriptionRequestHandler(
                monitor,
                resourceDescriptionRequestHandlerSettingsFactoryResult.getSettings(),
                assetIndex,
                transformerRegistry
        );
    }

    private RepresentationDescriptionRequestHandler createRepresentationDescriptionRequestHandler(
            SettingResolver settingResolver,
            AssetIndex assetIndex,
            TransformerRegistry transformerRegistry) {
        RepresentationDescriptionRequestHandlerSettingsFactory representationDescriptionRequestHandlerSettingsFactory = new RepresentationDescriptionRequestHandlerSettingsFactory(settingResolver);
        RepresentationDescriptionRequestHandlerSettingsFactoryResult representationDescriptionRequestHandlerSettingsFactoryResult = representationDescriptionRequestHandlerSettingsFactory.getSettingsResult();

        if (!representationDescriptionRequestHandlerSettingsFactoryResult.getErrors().isEmpty()) {
            throw new EdcException(String.format("Could not set up RepresentationDescriptionRequestHandler: %s", String.join(", ", representationDescriptionRequestHandlerSettingsFactoryResult.getErrors())));
        }

        return new RepresentationDescriptionRequestHandler(
                monitor,
                representationDescriptionRequestHandlerSettingsFactoryResult.getSettings(),
                assetIndex,
                transformerRegistry
        );
    }

    private DataCatalogDescriptionRequestHandler createDataCatalogDescriptionRequestHandler(
            SettingResolver settingResolver,
            DataCatalogService dataCatalogService,
            TransformerRegistry transformerRegistry) {
        DataCatalogDescriptionRequestHandlerSettingsFactory dataCatalogDescriptionRequestHandlerSettingsFactory = new DataCatalogDescriptionRequestHandlerSettingsFactory(settingResolver);
        DataCatalogDescriptionRequestHandlerSettingsFactoryResult dataCatalogDescriptionRequestHandlerSettingsFactoryResult = dataCatalogDescriptionRequestHandlerSettingsFactory.getSettingsResult();

        if (!dataCatalogDescriptionRequestHandlerSettingsFactoryResult.getErrors().isEmpty()) {
            throw new EdcException(String.format("Could not set up DataCatalogDescriptionRequestHandler: %s", String.join(", ", dataCatalogDescriptionRequestHandlerSettingsFactoryResult.getErrors())));
        }

        return new DataCatalogDescriptionRequestHandler(
                monitor,
                dataCatalogDescriptionRequestHandlerSettingsFactoryResult.getSettings(),
                dataCatalogService,
                transformerRegistry
        );
    }

    private ArtifactDescriptionRequestHandler createArtifactDescriptionRequestHandler(
            SettingResolver settingResolver,
            AssetIndex assetIndex,
            TransformerRegistry transformerRegistry
    ) {
        ArtifactDescriptionRequestHandlerSettingsFactory artifactDescriptionRequestHandlerSettingsFactory = new ArtifactDescriptionRequestHandlerSettingsFactory(settingResolver);
        ArtifactDescriptionRequestHandlerSettingsFactoryResult artifactDescriptionRequestHandlerSettingsFactoryResult = artifactDescriptionRequestHandlerSettingsFactory.getSettingsResult();

        if (!artifactDescriptionRequestHandlerSettingsFactoryResult.getErrors().isEmpty()) {
            throw new EdcException(String.format("Could not set up ArtifactDescriptionRequestHandler: %s", String.join(", ", artifactDescriptionRequestHandlerSettingsFactoryResult.getErrors())));
        }

        return new ArtifactDescriptionRequestHandler(
                monitor,
                artifactDescriptionRequestHandlerSettingsFactoryResult.getSettings(),
                assetIndex,
                transformerRegistry
        );
    }

    private MultipartController createMultipartController(SettingResolver settingResolver, IdentityService identityService, List<Handler> handlers) {
        MultipartControllerSettingsFactory multipartControllerSettingsFactory = new MultipartControllerSettingsFactory(settingResolver);
        MultipartControllerSettingsFactoryResult multipartControllerSettingsFactoryResult = multipartControllerSettingsFactory.createRejectionMessageFactorySettings();

        if (!multipartControllerSettingsFactoryResult.getErrors().isEmpty()) {
            throw new EdcException(String.format("Could not set up MultipartController: %s", String.join(", ", multipartControllerSettingsFactoryResult.getErrors())));
        }

        return new MultipartController(
                multipartControllerSettingsFactoryResult.getSettings(),
                identityService,
                handlers
        );
    }
}

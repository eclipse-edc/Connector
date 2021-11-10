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
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.service.DataCatalogService;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * ServiceExtension providing IDS multipart related API controllers
 */
public final class IdsMultipartApiServiceExtension implements ServiceExtension {

    @EdcSetting
    public static final String EDC_IDS_ID = "edc.ids.id";
    public static final String DEFAULT_EDC_IDS_ID = "urn:connector:edc";

    private static final String NAME = "IDS Multipart API extension";

    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of(IdentityService.FEATURE,
                "edc:ids:core",
                "edc:ids:transform:v1");
    }

    @Override
    public Set<String> provides() {
        return Set.of("edc:ids:api:multipart:endpoint:v1");
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
        ContractOfferService contractOfferService = serviceExtensionContext.getService(ContractOfferService.class);

        String connectorId = resolveConnectorId(serviceExtensionContext);

        // create description request handlers
        ArtifactDescriptionRequestHandler artifactDescriptionRequestHandler = new ArtifactDescriptionRequestHandler(monitor, connectorId, assetIndex, transformerRegistry);
        DataCatalogDescriptionRequestHandler dataCatalogDescriptionRequestHandler = new DataCatalogDescriptionRequestHandler(monitor, connectorId, dataCatalogService, transformerRegistry);
        RepresentationDescriptionRequestHandler representationDescriptionRequestHandler = new RepresentationDescriptionRequestHandler(monitor, connectorId, assetIndex, transformerRegistry);
        ResourceDescriptionRequestHandler resourceDescriptionRequestHandler = new ResourceDescriptionRequestHandler(monitor, connectorId, assetIndex, contractOfferService, transformerRegistry);
        ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler = new ConnectorDescriptionRequestHandler(monitor, connectorId, connectorService, transformerRegistry);

        // create request handler
        DescriptionHandler descriptionHandler = new DescriptionHandler(
                monitor,
                connectorId,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler);

        List<Handler> handlers = new LinkedList<>();
        handlers.add(descriptionHandler);

        // create & register controller
        MultipartController multipartController = new MultipartController(connectorId, identityService, handlers);
        webService.registerController(multipartController);
    }

    private String resolveConnectorId(@NotNull ServiceExtensionContext context) {
        Objects.requireNonNull(context);

        String value = context.getSetting(EDC_IDS_ID, null);

        if (value == null) {
            String message = "IDS Settings: No setting found for key '%s'. Using default value '%s'";
            monitor.warning(String.format(message, EDC_IDS_ID, DEFAULT_EDC_IDS_ID));
            value = DEFAULT_EDC_IDS_ID;
        }

        try {
            // Hint: use stringified uri to keep uri path and query
            IdsId idsId = IdsIdParser.parse(value);
            if (idsId != null && idsId.getType() == IdsType.CONNECTOR) {
                return idsId.getValue();
            }
        } catch (IllegalArgumentException e) {
            String message = "IDS Settings: Expected valid URN for setting '%s', but was %s'. Expected format: 'urn:connector:[id]'";
            throw new EdcException(String.format(message, EDC_IDS_ID, DEFAULT_EDC_IDS_ID));
        }

        return value;
    }
}

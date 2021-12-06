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
 *       Fraunhofer Institute for Software and Systems Engineering - add contract and notification message handlers
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartController;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ArtifactRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ContractAgreementHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ContractOfferHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ContractRejectionHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ContractRequestHandler;
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
import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
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

    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of("edc:ids:api:multipart:endpoint:v1");
    }

    @Override
    public Set<String> requires() {
        return Set.of(IdentityService.FEATURE,
                "edc:ids:core",
                AssetIndex.FEATURE,
                ContractNegotiationStore.FEATURE,
                "edc:ids:transform:v1",
                "edc:core:contract");
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        registerControllers(serviceExtensionContext);
    }

    private void registerControllers(ServiceExtensionContext serviceExtensionContext) {
        WebService webService = serviceExtensionContext.getService(WebService.class);
        IdentityService identityService = serviceExtensionContext.getService(IdentityService.class);
        CatalogService dataCatalogService = serviceExtensionContext.getService(CatalogService.class);
        ConnectorService connectorService = serviceExtensionContext.getService(ConnectorService.class);
        AssetIndex assetIndex = serviceExtensionContext.getService(AssetIndex.class);
        TransformerRegistry transformerRegistry = serviceExtensionContext.getService(TransformerRegistry.class);
        ContractOfferService contractOfferService = serviceExtensionContext.getService(ContractOfferService.class);
        ContractNegotiationStore contractNegotiationStore = serviceExtensionContext.getService(ContractNegotiationStore.class);

        String connectorId = resolveConnectorId(serviceExtensionContext);

        // create description request handlers
        ArtifactDescriptionRequestHandler artifactDescriptionRequestHandler = new ArtifactDescriptionRequestHandler(monitor, connectorId, assetIndex, transformerRegistry);
        DataCatalogDescriptionRequestHandler dataCatalogDescriptionRequestHandler = new DataCatalogDescriptionRequestHandler(monitor, connectorId, dataCatalogService, transformerRegistry);
        RepresentationDescriptionRequestHandler representationDescriptionRequestHandler = new RepresentationDescriptionRequestHandler(monitor, connectorId, assetIndex, transformerRegistry);
        ResourceDescriptionRequestHandler resourceDescriptionRequestHandler = new ResourceDescriptionRequestHandler(monitor, connectorId, assetIndex, contractOfferService, transformerRegistry);
        ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler = new ConnectorDescriptionRequestHandler(monitor, connectorId, connectorService, transformerRegistry);

        // create & register controller
        // TODO ObjectMapper needs to be replaced by one capable to write proper IDS JSON-LD
        //      once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

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

        TransferProcessManager transferProcessManager = serviceExtensionContext.getService(TransferProcessManager.class);
        ContractValidationService contractValidationService = serviceExtensionContext.getService(ContractValidationService.class);
        Vault vault = serviceExtensionContext.getService(Vault.class);
        ArtifactRequestHandler artifactRequestHandler = new ArtifactRequestHandler(monitor, connectorId, objectMapper, contractNegotiationStore, contractValidationService, transferProcessManager, vault);
        handlers.add(artifactRequestHandler);

        // create contract message handlers
        var providerNegotiationManager = serviceExtensionContext.getService(ProviderContractNegotiationManager.class);
        var consumerNegotiationManager = serviceExtensionContext.getService(ConsumerContractNegotiationManager.class);
        handlers.add(new ContractRequestHandler(monitor, connectorId, objectMapper, providerNegotiationManager, transformerRegistry, assetIndex));
        handlers.add(new ContractAgreementHandler(monitor, connectorId, objectMapper, consumerNegotiationManager, transformerRegistry, assetIndex));
        handlers.add(new ContractOfferHandler(monitor, connectorId, objectMapper, providerNegotiationManager, consumerNegotiationManager));
        handlers.add(new ContractRejectionHandler(monitor, connectorId, providerNegotiationManager, consumerNegotiationManager));

        // create & register controller
        MultipartController multipartController = new MultipartController(connectorId, objectMapper, identityService, handlers);
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

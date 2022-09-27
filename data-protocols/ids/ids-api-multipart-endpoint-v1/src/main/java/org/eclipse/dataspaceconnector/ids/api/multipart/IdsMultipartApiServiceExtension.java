/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH, Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       Daimler TSS GmbH - introduce factory to create RequestInProcessMessage
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *       Fraunhofer Institute for Software and Systems Engineering - Refactoring
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import org.eclipse.dataspaceconnector.ids.api.configuration.IdsApiConfiguration;
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartController;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ArtifactRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ContractAgreementHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ContractRejectionHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.ContractRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.EndpointDataReferenceHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformerRegistry;

import java.util.LinkedList;

import static org.eclipse.dataspaceconnector.ids.core.util.ConnectorIdUtil.resolveConnectorId;

/**
 * ServiceExtension providing IDS multipart related API controllers
 */
public final class IdsMultipartApiServiceExtension implements ServiceExtension {

    @Inject
    private Monitor monitor;

    @Inject
    private WebService webService;

    @Inject
    private DynamicAttributeTokenService dynamicAttributeTokenService;

    @Inject
    private CatalogService dataCatalogService;

    @Inject
    private ConnectorService connectorService;

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private IdsTransformerRegistry transformerRegistry;

    @Inject
    private ContractOfferService contractOfferService;

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Inject
    private ConsumerContractNegotiationManager consumerNegotiationManager;

    @Inject
    private ProviderContractNegotiationManager providerNegotiationManager;

    @Inject
    private TransferProcessManager transferProcessManager;

    @Inject
    private ContractValidationService contractValidationService;

    @Inject
    private EndpointDataReferenceReceiverRegistry endpointDataReferenceReceiverRegistry;

    @Inject
    private EndpointDataReferenceTransformerRegistry endpointDataReferenceTransformerRegistry;

    @Inject
    private IdsApiConfiguration idsApiConfiguration;

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return "IDS Multipart API";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerControllers(context);
    }

    private void registerControllers(ServiceExtensionContext context) {
        var connectorId = resolveConnectorId(context);

        var objectMapper = context.getTypeManager().getMapper("ids");

        // create request handlers
        var handlers = new LinkedList<Handler>();
        handlers.add(new DescriptionRequestHandler(monitor, connectorId, transformerRegistry, assetIndex, dataCatalogService, contractOfferService, connectorService));
        handlers.add(new ArtifactRequestHandler(monitor, connectorId, objectMapper, contractNegotiationStore, contractValidationService, transferProcessManager, vault));
        handlers.add(new EndpointDataReferenceHandler(monitor, connectorId, endpointDataReferenceReceiverRegistry, endpointDataReferenceTransformerRegistry, context.getTypeManager()));
        handlers.add(new ContractRequestHandler(monitor, connectorId, objectMapper, providerNegotiationManager, transformerRegistry, assetIndex));
        handlers.add(new ContractAgreementHandler(monitor, connectorId, objectMapper, consumerNegotiationManager, transformerRegistry));
        handlers.add(new ContractRejectionHandler(monitor, connectorId, providerNegotiationManager, consumerNegotiationManager));

        // create & register controller
        var multipartController = new MultipartController(monitor, connectorId, objectMapper, dynamicAttributeTokenService, handlers, idsApiConfiguration.getIdsWebhookAddress());
        webService.registerResource(idsApiConfiguration.getContextAlias(), multipartController);
    }

}

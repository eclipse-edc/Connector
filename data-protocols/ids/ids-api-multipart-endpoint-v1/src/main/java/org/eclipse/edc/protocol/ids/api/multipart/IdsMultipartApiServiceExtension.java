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

package org.eclipse.edc.protocol.ids.api.multipart;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfiguration;
import org.eclipse.edc.protocol.ids.api.multipart.controller.MultipartController;
import org.eclipse.edc.protocol.ids.api.multipart.handler.ArtifactRequestHandler;
import org.eclipse.edc.protocol.ids.api.multipart.handler.ContractAgreementHandler;
import org.eclipse.edc.protocol.ids.api.multipart.handler.ContractRejectionHandler;
import org.eclipse.edc.protocol.ids.api.multipart.handler.ContractRequestHandler;
import org.eclipse.edc.protocol.ids.api.multipart.handler.DescriptionRequestHandler;
import org.eclipse.edc.protocol.ids.api.multipart.handler.EndpointDataReferenceHandler;
import org.eclipse.edc.protocol.ids.api.multipart.handler.Handler;
import org.eclipse.edc.protocol.ids.spi.service.CatalogService;
import org.eclipse.edc.protocol.ids.spi.service.ConnectorService;
import org.eclipse.edc.protocol.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

import java.util.LinkedList;

import static org.eclipse.edc.protocol.ids.util.ConnectorIdUtil.resolveConnectorId;

/**
 * ServiceExtension providing IDS multipart related API controllers
 */
@Extension(value = IdsMultipartApiServiceExtension.NAME)
public final class IdsMultipartApiServiceExtension implements ServiceExtension {

    public static final String NAME = "IDS Multipart API";
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
    private ContractOfferResolver contractOfferResolver;

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Inject
    private EndpointDataReferenceReceiverRegistry endpointDataReferenceReceiverRegistry;

    @Inject
    private EndpointDataReferenceTransformerRegistry endpointDataReferenceTransformerRegistry;

    @Inject
    private IdsApiConfiguration idsApiConfiguration;

    @Inject
    private Vault vault;
    
    @Inject
    private TransferProcessService transferProcessService;

    @Inject
    private TypeManager typeManager;

    @Inject
    private ContractNegotiationService contractNegotiationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerControllers(context);
    }

    private void registerControllers(ServiceExtensionContext context) {
        var connectorId = resolveConnectorId(context);

        var objectMapper = typeManager.getMapper("ids");

        var handlers = new LinkedList<Handler>();
        handlers.add(new DescriptionRequestHandler(monitor, connectorId, transformerRegistry, assetIndex, dataCatalogService, contractOfferResolver, connectorService, objectMapper));
        handlers.add(new ArtifactRequestHandler(monitor, connectorId, objectMapper, contractNegotiationStore, vault, transferProcessService));
        handlers.add(new EndpointDataReferenceHandler(monitor, connectorId, endpointDataReferenceReceiverRegistry, endpointDataReferenceTransformerRegistry, typeManager));
        handlers.add(new ContractRequestHandler(monitor, connectorId, objectMapper, transformerRegistry, assetIndex, contractNegotiationService));
        handlers.add(new ContractAgreementHandler(monitor, connectorId, objectMapper, transformerRegistry, contractNegotiationService));
        handlers.add(new ContractRejectionHandler(monitor, connectorId, contractNegotiationService));

        var multipartController = new MultipartController(monitor, connectorId, objectMapper, dynamicAttributeTokenService, handlers, idsApiConfiguration.getIdsWebhookAddress());
        webService.registerResource(idsApiConfiguration.getContextAlias(), multipartController);
    }

}

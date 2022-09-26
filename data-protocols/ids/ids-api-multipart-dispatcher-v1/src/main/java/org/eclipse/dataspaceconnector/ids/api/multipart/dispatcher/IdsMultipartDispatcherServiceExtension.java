/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.api.configuration.IdsApiConfiguration;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.IdsMultipartSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartArtifactRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartCatalogDescriptionRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartContractAgreementSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartContractOfferSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartContractRejectionSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartDescriptionRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type.MultipartEndpointDataReferenceRequestSender;
import org.eclipse.dataspaceconnector.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.net.URI;

import static org.eclipse.dataspaceconnector.ids.core.util.ConnectorIdUtil.resolveConnectorId;

public class IdsMultipartDispatcherServiceExtension implements ServiceExtension {

    @Inject
    private Monitor monitor;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private DynamicAttributeTokenService dynamicAttributeTokenService;

    @Inject
    private IdsTransformerRegistry transformerRegistry;

    @Inject
    private IdsApiConfiguration idsApiConfiguration;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return "IDS Multipart Dispatcher API";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var connectorId = resolveConnectorId(context);
        var idsWebhookAddress = idsApiConfiguration.getIdsWebhookAddress();

        var objectMapper = context.getTypeManager().getMapper("ids");
        var typeManager = context.getTypeManager();

        var senderContext = new SenderDelegateContext(URI.create(connectorId), objectMapper, transformerRegistry, idsWebhookAddress);

        var sender = new IdsMultipartSender(monitor, httpClient, dynamicAttributeTokenService, objectMapper);
        var dispatcher = new IdsMultipartRemoteMessageDispatcher(sender);
        dispatcher.register(new MultipartArtifactRequestSender(senderContext, vault));
        dispatcher.register(new MultipartDescriptionRequestSender(senderContext));
        dispatcher.register(new MultipartContractOfferSender(senderContext));
        dispatcher.register(new MultipartContractAgreementSender(senderContext));
        dispatcher.register(new MultipartContractRejectionSender(senderContext));
        dispatcher.register(new MultipartCatalogDescriptionRequestSender(senderContext));
        dispatcher.register(new MultipartEndpointDataReferenceRequestSender(senderContext, typeManager));

        dispatcherRegistry.register(dispatcher);
    }

}

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

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher;

import org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfiguration;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.IdsMultipartSender;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type.MultipartArtifactRequestSender;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type.MultipartCatalogDescriptionRequestSender;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type.MultipartContractAgreementSender;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type.MultipartContractOfferSender;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type.MultipartContractRejectionSender;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type.MultipartDescriptionRequestSender;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type.MultipartEndpointDataReferenceRequestSender;
import org.eclipse.edc.protocol.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.protocol.ids.util.ConnectorIdUtil.resolveConnectorId;

@Extension(value = IdsMultipartDispatcherServiceExtension.NAME)
public class IdsMultipartDispatcherServiceExtension implements ServiceExtension {

    public static final String NAME = "IDS Multipart Dispatcher API";
    @Inject
    private Monitor monitor;

    @Inject
    private EdcHttpClient httpClient;

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
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var connectorId = resolveConnectorId(context);
        var idsWebhookAddress = idsApiConfiguration.getIdsWebhookAddress();

        var objectMapper = context.getTypeManager().getMapper("ids");
        var typeManager = context.getTypeManager();

        var senderContext = new SenderDelegateContext(connectorId, objectMapper, transformerRegistry, idsWebhookAddress);

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

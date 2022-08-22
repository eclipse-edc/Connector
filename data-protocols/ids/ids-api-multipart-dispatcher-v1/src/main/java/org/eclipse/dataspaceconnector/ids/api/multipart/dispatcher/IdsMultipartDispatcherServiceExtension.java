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
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartArtifactRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartCatalogDescriptionRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartContractAgreementSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartContractOfferSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartContractRejectionSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartDescriptionRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.MultipartEndpointDataReferenceRequestSender;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsType;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class IdsMultipartDispatcherServiceExtension implements ServiceExtension {

    @EdcSetting
    public static final String EDC_IDS_ID = "edc.ids.id";
    public static final String DEFAULT_EDC_IDS_ID = "urn:connector:edc";

    @Inject
    private Monitor monitor;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private IdentityService identityService;

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

        var dispatcher = new IdsMultipartRemoteMessageDispatcher();
        dispatcher.register(new MultipartArtifactRequestSender(connectorId, httpClient, objectMapper, monitor, vault, identityService, transformerRegistry, idsWebhookAddress));
        dispatcher.register(new MultipartDescriptionRequestSender(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry));
        dispatcher.register(new MultipartContractOfferSender(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry, idsWebhookAddress));
        dispatcher.register(new MultipartContractAgreementSender(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry, idsWebhookAddress));
        dispatcher.register(new MultipartContractRejectionSender(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry));
        dispatcher.register(new MultipartCatalogDescriptionRequestSender(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry));
        dispatcher.register(new MultipartEndpointDataReferenceRequestSender(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry, typeManager));

        dispatcherRegistry.register(dispatcher);
    }

    private String resolveConnectorId(@NotNull ServiceExtensionContext context) {
        Objects.requireNonNull(context);

        var value = context.getSetting(EDC_IDS_ID, DEFAULT_EDC_IDS_ID);

        // Hint: use stringified uri to keep uri path and query
        var result = IdsId.from(value);
        if (result.succeeded()) {
            var idsId = result.getContent();
            if (idsId.getType() == IdsType.CONNECTOR) {
                return idsId.getValue();
            }
        } else {
            var message = "IDS Settings: Expected valid URN for setting '%s', but was %s'. Expected format: 'urn:connector:[id]'";
            throw new EdcException(String.format(message, EDC_IDS_ID, DEFAULT_EDC_IDS_ID));
        }

        return value;
    }

}

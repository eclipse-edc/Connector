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
import org.eclipse.dataspaceconnector.ids.core.policy.IdsConstraintImpl;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.domain.DefaultValues;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.serializer.jsonld.JsonldSerializer;
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

    @Inject private Monitor monitor;
    @Inject private OkHttpClient httpClient;
    @Inject private IdentityService identityService;
    @Inject private IdsTransformerRegistry transformerRegistry;
    @Inject private IdsApiConfiguration idsApiConfiguration;
    @Inject private Vault vault;
    @Inject private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Override
    public String name() {
        return "IDS Multipart Dispatcher API";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var connectorId = resolveConnectorId(context);
        var idsWebhookAddress = idsApiConfiguration.getIdsWebhookAddress();

        // customize serializer
        var serializer = new JsonldSerializer(monitor);
        serializer.setContext(DefaultValues.CONTEXT);
        serializer.setSubtypes(IdsConstraintImpl.class);

        var dispatcher = new IdsMultipartRemoteMessageDispatcher();
        dispatcher.register(new MultipartArtifactRequestSender(connectorId, httpClient, serializer, monitor, vault, identityService, transformerRegistry, idsWebhookAddress));
        dispatcher.register(new MultipartDescriptionRequestSender(connectorId, httpClient, serializer, monitor, identityService, transformerRegistry));
        dispatcher.register(new MultipartContractOfferSender(connectorId, httpClient, serializer, monitor, identityService, transformerRegistry, idsWebhookAddress));
        dispatcher.register(new MultipartContractAgreementSender(connectorId, httpClient, serializer, monitor, identityService, transformerRegistry, idsWebhookAddress));
        dispatcher.register(new MultipartContractRejectionSender(connectorId, httpClient, serializer, monitor, identityService, transformerRegistry));
        dispatcher.register(new MultipartCatalogDescriptionRequestSender(connectorId, httpClient, serializer, monitor, identityService, transformerRegistry));
        dispatcher.register(new MultipartEndpointDataReferenceRequestSender(connectorId, httpClient, serializer, monitor, identityService, transformerRegistry));

        dispatcherRegistry.register(dispatcher);
    }

    private String resolveConnectorId(@NotNull ServiceExtensionContext context) {
        Objects.requireNonNull(context);

        var value = getSetting(context, EDC_IDS_ID, DEFAULT_EDC_IDS_ID);
        try {
            // Hint: use stringified uri to keep uri path and query
            IdsId idsId = IdsIdParser.parse(value);
            if (idsId != null && idsId.getType() == IdsType.CONNECTOR) {
                return idsId.getValue();
            }
        } catch (IllegalArgumentException e) {
            var msg = "IDS Settings: Expected valid URN for setting '%s', but was %s'. Expected format: 'urn:connector:[id]'";
            throw new EdcException(String.format(msg, EDC_IDS_ID, DEFAULT_EDC_IDS_ID));
        }

        return value;
    }

    @NotNull
    private String getSetting(@NotNull ServiceExtensionContext context, String key, String defaultValue) {
        var value = context.getSetting(key, null);
        if (value == null) {
            var msg = "IDS Settings: No setting found for key '%s'. Using default value '%s'";
            monitor.warning(String.format(msg, key, defaultValue));
            return defaultValue;
        } else {
            return value;
        }
    }

}

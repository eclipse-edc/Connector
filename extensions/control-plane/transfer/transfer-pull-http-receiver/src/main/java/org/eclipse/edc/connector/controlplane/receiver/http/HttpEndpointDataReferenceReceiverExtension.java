/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.receiver.http;

import org.eclipse.edc.connector.controlplane.transfer.spi.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.util.string.StringUtils;

@Extension(value = HttpEndpointDataReferenceReceiverExtension.NAME)
public class HttpEndpointDataReferenceReceiverExtension implements ServiceExtension {

    public static final String NAME = "Http Endpoint Data Reference Receiver";
    @Setting
    private static final String HTTP_RECEIVER_ENDPOINT = "edc.receiver.http.endpoint";
    @Setting
    private static final String HTTP_RECEIVER_AUTH_KEY = "edc.receiver.http.auth-key";
    @Setting
    private static final String HTTP_RECEIVER_AUTH_CODE = "edc.receiver.http.auth-code";

    @Inject
    private EndpointDataReferenceReceiverRegistry receiverRegistry;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var endpoint = context.getSetting(HTTP_RECEIVER_ENDPOINT, null);
        if (StringUtils.isNullOrBlank(endpoint)) {
            throw new EdcException(String.format("Missing mandatory http receiver endpoint: `%s`", HTTP_RECEIVER_ENDPOINT));
        }
        var authKey = context.getSetting(HTTP_RECEIVER_AUTH_KEY, null);
        var authCode = context.getSetting(HTTP_RECEIVER_AUTH_CODE, null);
        var receiver = HttpEndpointDataReferenceReceiver.Builder.newInstance()
                .endpoint(endpoint)
                .authHeader(authKey, authCode)
                .httpClient(httpClient)
                .typeManager(typeManager)
                .monitor(context.getMonitor())
                .build();
        receiverRegistry.registerReceiver(receiver);
    }

}

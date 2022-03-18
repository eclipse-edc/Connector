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

package org.eclipse.dataspaceconnector.receiver.http;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;

public class HttpEndpointDataReferenceReceiverExtension implements ServiceExtension {

    @EdcSetting
    private static final String HTTP_RECEIVER_ENDPOINT = "edc.receiver.http.endpoint";

    @EdcSetting
    private static final String HTTP_RECEIVER_AUTH_KEY = "edc.receiver.http.auth-key";

    @EdcSetting
    private static final String HTTP_RECEIVER_AUTH_CODE = "edc.receiver.http.auth-code";

    @Inject
    private EndpointDataReferenceReceiverRegistry receiverRegistry;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    @SuppressWarnings("rawtypes")
    private RetryPolicy retryPolicy;

    @Override
    public String name() {
        return "Http Endpoint Data Reference Receiver";
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
                .typeManager(context.getTypeManager())
                .retryPolicy(retryPolicy)
                .monitor(context.getMonitor())
                .build();
        receiverRegistry.addReceiver(receiver);
    }

}

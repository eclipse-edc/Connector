/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.receiver.http.dynamic;

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = HttpDynamicEndpointDataReferenceReceiverExtension.NAME)
public class HttpDynamicEndpointDataReferenceReceiverExtension implements ServiceExtension {

    public static final String NAME = "Http Dynamic Endpoint Data Reference Receiver";

    @Setting(value = "Header name that will be sent with the EDR")
    private static final String HTTP_RECEIVER_AUTH_KEY = "edc.receiver.http.dynamic.auth-key";
    @Setting(value = "Header value that will be sent with the EDR")
    private static final String HTTP_RECEIVER_AUTH_CODE = "edc.receiver.http.dynamic.auth-code";

    @Inject
    private EndpointDataReferenceReceiverRegistry receiverRegistry;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private RetryPolicy<Object> retryPolicy;


    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private TransferProcessObservable transferProcessObservable;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var authKey = context.getSetting(HTTP_RECEIVER_AUTH_KEY, null);
        var authCode = context.getSetting(HTTP_RECEIVER_AUTH_CODE, null);

        var receiver = HttpDynamicEndpointDataReferenceReceiver.Builder.newInstance()
                .httpClient(httpClient)
                .typeManager(context.getTypeManager())
                .retryPolicy(retryPolicy)
                .authHeader(authKey, authCode)
                .monitor(context.getMonitor())
                .transferProcessStore(transferProcessStore)
                .build();

        receiverRegistry.registerReceiver(receiver);
    }

}

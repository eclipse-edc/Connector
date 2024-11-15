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

package org.eclipse.edc.connector.controlplane.receiver.http.dynamic;

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.controlplane.transfer.spi.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(value = HttpDynamicEndpointDataReferenceReceiverExtension.NAME)
public class HttpDynamicEndpointDataReferenceReceiverExtension implements ServiceExtension {

    public static final String NAME = "Http Dynamic Endpoint Data Reference Receiver";

    @Setting(description = "Fallback endpoint when url is missing the the transfer process", key = "edc.receiver.http.dynamic.endpoint", required = false)
    private String fallbackEndpoint;

    @Setting(description = "Header name that will be sent with the EDR", key = "edc.receiver.http.dynamic.auth-key", required = false)
    private String authKey;

    @Setting(description = "Header value that will be sent with the EDR", key = "edc.receiver.http.dynamic.auth-code", required = false)
    private String authCode;

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

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var receiver = HttpDynamicEndpointDataReferenceReceiver.Builder.newInstance()
                .httpClient(httpClient)
                .typeManager(typeManager)
                .retryPolicy(retryPolicy)
                .fallbackEndpoint(fallbackEndpoint)
                .authHeader(authKey, authCode)
                .monitor(context.getMonitor())
                .transferProcessStore(transferProcessStore)
                .build();

        receiverRegistry.registerReceiver(receiver);
    }

}

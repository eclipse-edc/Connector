/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.dispatcher;

import okhttp3.HttpUrl;
import okhttp3.Request;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.RequestBasePathProvider;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.RequestFactory;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.RequestPathProvider;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Creates a GET request through the Dataspace Protocol
 *
 * @param <M> the message class.
 */
public class GetDspHttpRequestFactory<M extends RemoteMessage> implements RequestFactory<M, Request> {
    private final RequestPathProvider<M> pathProvider;
    private final RequestBasePathProvider dspBasePathProvider;

    public GetDspHttpRequestFactory(RequestBasePathProvider dspBasePathProvider, RequestPathProvider<M> pathProvider) {
        this.dspBasePathProvider = dspBasePathProvider;
        this.pathProvider = pathProvider;
    }

    @Override
    public Request createRequest(M message) {

        var url = HttpUrl.get(dspBasePathProvider.provideBasePath(message) + pathProvider.providePath(message));

        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }
}
